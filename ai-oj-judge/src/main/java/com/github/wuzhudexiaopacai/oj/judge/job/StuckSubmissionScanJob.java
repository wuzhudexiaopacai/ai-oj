package com.github.wuzhudexiaopacai.oj.judge.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import com.github.wuzhudexiaopacai.oj.judge.mq.JudgeSubmitMessage;
import com.github.wuzhudexiaopacai.oj.judge.service.JudgeExecuteService;
import com.github.wuzhudexiaopacai.oj.judge.statemachine.JudgeStateMachineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 卡死提交扫描任务（Quartz 集群，每分钟扫描非终态且超时未更新的提交）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class StuckSubmissionScanJob extends QuartzJobBean {

    private final JudgeSubmissionMapper judgeSubmissionMapper;

    private final JudgeStateMachineService stateMachineService;

    private final JudgeExecuteService judgeExecuteService;

    private final RocketMQTemplate rocketMQTemplate;

    @Value("${ai-oj.judge.judge-timeout-seconds:600}")
    private long judgeTimeoutSeconds;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        LocalDateTime deadline = LocalDateTime.now().minusSeconds(judgeTimeoutSeconds);
        List<Integer> nonTerminalCodes = JudgeStatusEnum.NON_TERMINAL.stream()
                .map(JudgeStatusEnum::getCode)
                .toList();
        List<JudgeSubmission> stuckList = judgeSubmissionMapper.selectList(new LambdaQueryWrapper<JudgeSubmission>()
                .in(JudgeSubmission::getStatus, nonTerminalCodes)
                .lt(JudgeSubmission::getUpdateTime, deadline)
                .last("limit 200"));
        log.info("卡死提交扫描完成，命中 {} 条", stuckList.size());

        for (JudgeSubmission submission : stuckList) {
            try {
                int retryCount = submission.getRetryCount() == null ? 0 : submission.getRetryCount();
                if (retryCount < MqConstants.JUDGE_MAX_RETRY) {
                    // 重试计数 +1 并重新投递；乐观锁冲突说明已有其他实例处理，跳过本条
                    submission.setRetryCount(retryCount + 1);
                    if (judgeSubmissionMapper.updateById(submission) == 0) {
                        log.warn("卡死提交 {} 状态已被并发修改，跳过", submission.getSubmitId());
                        continue;
                    }
                    JudgeSubmitMessage message = JudgeSubmitMessage.builder()
                            .submitId(submission.getSubmitId())
                            .submissionId(submission.getId())
                            .build();
                    rocketMQTemplate.syncSend(MqConstants.JUDGE_SUBMIT_TOPIC,
                            MessageBuilder.withPayload(message)
                                    .setHeader(RocketMQHeaders.KEYS, submission.getSubmitId())
                                    .build(),
                            5000);
                    log.info("卡死提交 {} 重新投递 retryCount={}", submission.getSubmitId(), submission.getRetryCount());
                } else {
                    // 超过重试上限：仅本次成功落库为 SYSTEM_ERROR 才广播；
                    // 若正常判题已并发落为其他终态，不能用内存中的旧记录广播错误结论
                    if (stateMachineService.transitionToTerminal(submission, JudgeStatusEnum.SYSTEM_ERROR,
                            "判题卡死超过重试上限，由补偿任务置为系统错误")) {
                        judgeExecuteService.broadcast(submission);
                        log.warn("提交 {} 卡死超限，置为 SYSTEM_ERROR", submission.getSubmitId());
                    }
                }
            } catch (Exception e) {
                log.error("卡死提交处理失败 submitId={}", submission.getSubmitId(), e);
            }
        }
    }
}
