package com.github.wuzhudexiaopacai.oj.judge.mq;

import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.judge.service.JudgeExecuteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 判题提交消费者
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(consumerGroup = MqConstants.GROUP_JUDGE, topic = MqConstants.JUDGE_SUBMIT_TOPIC)
public class JudgeSubmitConsumer implements RocketMQListener<JudgeSubmitMessage> {

    private final JudgeExecuteService judgeExecuteService;

    @Override
    public void onMessage(JudgeSubmitMessage message) {
        log.info("收到判题消息 submitId={} submissionId={}", message.getSubmitId(), message.getSubmissionId());
        judgeExecuteService.execute(message.getSubmissionId());
    }
}
