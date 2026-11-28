package com.github.wuzhudexiaopacai.oj.judge.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import com.github.wuzhudexiaopacai.oj.judge.entity.BizDeadLetter;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import com.github.wuzhudexiaopacai.oj.judge.mapper.BizDeadLetterMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import com.github.wuzhudexiaopacai.oj.judge.service.JudgeExecuteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 判题死信消费者（归档 + 兜底置 SYSTEM_ERROR）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(consumerGroup = MqConstants.GROUP_JUDGE_DLQ, topic = "%DLQ%" + MqConstants.GROUP_JUDGE)
public class JudgeDlqConsumer implements RocketMQListener<MessageExt> {

    private final BizDeadLetterMapper bizDeadLetterMapper;

    private final JudgeSubmissionMapper judgeSubmissionMapper;

    private final JudgeExecuteService judgeExecuteService;

    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(MessageExt messageExt) {
        String msgId = messageExt.getMsgId();
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.error("收到判题死信 msgId={} body={}", msgId, body);

        // 1. 兜底终态并广播：仅本次成功置为终态才广播；
        //    并发冲突时正常判题结果已落库，不能再广播旧记录对应的 SYSTEM_ERROR
        try {
            JudgeSubmitMessage submitMessage = objectMapper.readValue(body, JudgeSubmitMessage.class);
            // 优先按提交记录主键定位；submitId 仅在同一用户内唯一，不能作为全局条件
            JudgeSubmission submission = submitMessage.getSubmissionId() == null
                    ? judgeSubmissionMapper.selectOne(new LambdaQueryWrapper<JudgeSubmission>()
                            .eq(JudgeSubmission::getSubmitId, submitMessage.getSubmitId()))
                    : judgeSubmissionMapper.selectById(submitMessage.getSubmissionId());
            if (submission != null
                    && !JudgeStatusEnum.fromCode(submission.getStatus()).isTerminal()) {
                if (judgeExecuteService.transitionToTerminalAndSaveOutbox(submission, JudgeStatusEnum.SYSTEM_ERROR,
                        "判题多次失败进入死信，已归档")) {
                    judgeExecuteService.broadcast(submission);
                }
            }
        } catch (Exception e) {
            log.error("死信解析/兜底失败 msgId={}", msgId, e);
        }

        // 2. 归档（uk_msg_id 幂等）：放在终态处理之后，避免归档成功但状态未兜底
        BizDeadLetter deadLetter = new BizDeadLetter();
        deadLetter.setTopic(MqConstants.JUDGE_SUBMIT_TOPIC);
        deadLetter.setConsumerGroup(MqConstants.GROUP_JUDGE);
        deadLetter.setMsgId(msgId);
        deadLetter.setMsgBody(body);
        deadLetter.setRetryTimes(messageExt.getReconsumeTimes());
        deadLetter.setStatus(0);
        try {
            bizDeadLetterMapper.insert(deadLetter);
        } catch (DuplicateKeyException e) {
            log.info("死信已归档，幂等跳过 msgId={}", msgId);
        }
    }
}
