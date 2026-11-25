package com.github.wuzhudexiaopacai.oj.judge.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.api.dto.JudgeResultMessage;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeOutbox;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeOutboxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 判题结果 Outbox 补投任务（Quartz 集群）：
 * 扫描投递失败的待投递记录，按指数退避重投，避免结果消息永久丢失
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class ResultOutboxResendJob extends QuartzJobBean {

    /**
     * 单次最多补投条数
     */
    private static final int BATCH_SIZE = 100;

    /**
     * 退避上限（秒）
     */
    private static final long MAX_BACKOFF_SECONDS = 300L;

    private final JudgeOutboxMapper judgeOutboxMapper;

    private final RocketMQTemplate rocketMQTemplate;

    private final ObjectMapper objectMapper;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        List<JudgeOutbox> pending = judgeOutboxMapper.selectList(new LambdaQueryWrapper<JudgeOutbox>()
                .eq(JudgeOutbox::getStatus, JudgeOutbox.STATUS_PENDING)
                .le(JudgeOutbox::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(JudgeOutbox::getId)
                .last("limit " + BATCH_SIZE));
        for (int i = 0; i < pending.size(); i++) {
            if (!resend(pending.get(i))) {
                // MQ 不可用时逐条重试只会反复等待发送超时并长时间占用调度线程，本轮直接结束
                log.warn("MQ 投递失败，终止本轮补投，剩余 {} 条留待下次调度", pending.size() - i);
                break;
            }
        }
        if (!pending.isEmpty()) {
            log.info("Outbox 本轮扫描 {} 条", pending.size());
        }
    }

    /**
     * 单条补投：成功标记已投递，发送失败按退避更新下次重试时间
     *
     * @return true 表示可继续处理后续记录；false 表示 MQ 不可用，应终止本轮
     */
    private boolean resend(JudgeOutbox outbox) {
        JudgeResultMessage message;
        try {
            message = objectMapper.readValue(outbox.getPayload(), JudgeResultMessage.class);
        } catch (Exception e) {
            // 脏数据不应阻塞其他记录补投
            markRetry(outbox, e);
            return true;
        }
        try {
            rocketMQTemplate.syncSend(MqConstants.JUDGE_RESULT_TOPIC,
                    MessageBuilder.withPayload(message)
                            .setHeader(RocketMQHeaders.KEYS, outbox.getSubmitId())
                            .build(),
                    5000);
            outbox.setStatus(JudgeOutbox.STATUS_SENT);
            judgeOutboxMapper.updateById(outbox);
            log.info("Outbox 补投成功 submitId={}", outbox.getSubmitId());
            return true;
        } catch (Exception e) {
            markRetry(outbox, e);
            return false;
        }
    }

    /**
     * 记录失败并按指数退避安排下次重试
     */
    private void markRetry(JudgeOutbox outbox, Exception e) {
        int retry = (outbox.getRetryCount() == null ? 0 : outbox.getRetryCount()) + 1;
        outbox.setRetryCount(retry);
        outbox.setNextRetryTime(LocalDateTime.now().plusSeconds(backoffSeconds(retry)));
        judgeOutboxMapper.updateById(outbox);
        log.error("Outbox 补投失败 submitId={} retry={}", outbox.getSubmitId(), retry, e);
    }

    /**
     * 指数退避：10s、20s、40s…，封顶 5 分钟
     *
     * @param retry 已重试次数（从 1 开始）
     * @return 退避秒数
     */
    static long backoffSeconds(int retry) {
        long delay = 10L << Math.min(Math.max(retry - 1, 0), 5);
        return Math.min(delay, MAX_BACKOFF_SECONDS);
    }
}
