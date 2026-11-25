package com.github.wuzhudexiaopacai.oj.ai.mq;

import com.github.wuzhudexiaopacai.oj.ai.constant.AiConstants;
import com.github.wuzhudexiaopacai.oj.ai.dto.AiTaskMessage;
import com.github.wuzhudexiaopacai.oj.ai.strategy.AiSceneStrategyFactory;
import com.github.wuzhudexiaopacai.oj.ai.vo.TaskStatusVO;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * AI 异步任务消费者（出题/批量评审等非实时任务，Redis 结果幂等）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = MqConstants.AI_TASK_TOPIC, consumerGroup = "ai-oj-ai-task-consumer")
public class AiTaskConsumer implements RocketMQListener<AiTaskMessage> {

    /**
     * 任务结果 Redis TTL：1 天
     */
    private static final long TASK_TTL_DAYS = 1L;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ChatClient chatClient;

    private final AiSceneStrategyFactory strategyFactory;

    @Override
    public void onMessage(AiTaskMessage message) {
        if (message == null || message.getTaskId() == null) {
            log.warn("AI 任务消息字段缺失，直接消费成功: {}", message);
            return;
        }
        String taskKey = AiConstants.AI_TASK_KEY + message.getTaskId();

        // 幂等：已有成功结果直接跳过重复消费
        Object cached = redisTemplate.opsForValue().get(taskKey);
        if (cached instanceof TaskStatusVO status && AiConstants.TASK_SUCCESS.equals(status.getStatus())) {
            log.info("AI 任务已完成，跳过重复消费: taskId={}", message.getTaskId());
            return;
        }

        try {
            String prompt = strategyFactory.getStrategy(message.getTaskType()).buildPrompt(message.getParams());
            String result = chatClient.prompt().messages(new UserMessage(prompt)).call().content();
            redisTemplate.opsForValue().set(taskKey,
                    new TaskStatusVO(message.getTaskId(), message.getUserId(), AiConstants.TASK_SUCCESS, result),
                    TASK_TTL_DAYS, TimeUnit.DAYS);
            log.info("AI 任务执行成功: taskId={}, taskType={}", message.getTaskId(), message.getTaskType());
        } catch (Exception e) {
            log.error("AI 任务执行失败: {}", message, e);
            redisTemplate.opsForValue().set(taskKey,
                    new TaskStatusVO(message.getTaskId(), message.getUserId(), AiConstants.TASK_FAILED,
                            "任务执行失败: " + e.getMessage()),
                    TASK_TTL_DAYS, TimeUnit.DAYS);
            // 重抛交由 RocketMQ 重试（FAILED 状态不阻断重试，成功后会覆盖）
            throw e;
        }
    }
}
