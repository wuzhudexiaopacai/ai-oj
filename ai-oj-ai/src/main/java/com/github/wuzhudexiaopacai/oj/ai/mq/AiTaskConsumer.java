package com.github.wuzhudexiaopacai.oj.ai.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.ai.constant.AiConstants;
import com.github.wuzhudexiaopacai.oj.ai.dto.AiTaskMessage;
import com.github.wuzhudexiaopacai.oj.ai.strategy.AiSceneStrategyFactory;
import com.github.wuzhudexiaopacai.oj.ai.vo.TaskStatusVO;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * AI 异步任务消费者（出题/批量评审等非实时任务，Redis 结果幂等）
 * <p>
 * 大模型调用成本高：失败只做有限次重试，超过上限后标记失败并确认消费，
 * 避免消息在 RocketMQ 中反复重投造成重复计费；无法解析的消息直接丢弃。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = MqConstants.AI_TASK_TOPIC, consumerGroup = MqConstants.GROUP_AI_TASK)
public class AiTaskConsumer implements RocketMQListener<MessageExt> {

    /**
     * 任务结果 Redis TTL：1 天
     */
    private static final long TASK_TTL_DAYS = 1L;

    /**
     * 失败最大重试次数（首次消费 reconsumeTimes=0，最多共 3 次尝试）
     */
    private static final int MAX_RETRY_TIMES = 2;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ChatClient chatClient;

    private final AiSceneStrategyFactory strategyFactory;

    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(MessageExt messageExt) {
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        AiTaskMessage message;
        try {
            message = objectMapper.readValue(body, AiTaskMessage.class);
        } catch (Exception e) {
            // 无法解析的消息永远不可能成功，直接确认消费，避免无效重试占用队列
            log.error("AI 任务消息解析失败，丢弃: msgId={}, body={}", messageExt.getMsgId(), body, e);
            return;
        }
        if (message == null || message.getTaskId() == null) {
            log.warn("AI 任务消息字段缺失，直接消费成功: {}", body);
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
            boolean exhausted = messageExt.getReconsumeTimes() >= MAX_RETRY_TIMES;
            log.error("AI 任务执行失败: taskId={}, reconsumeTimes={}, giveUp={}",
                    message.getTaskId(), messageExt.getReconsumeTimes(), exhausted, e);
            redisTemplate.opsForValue().set(taskKey,
                    new TaskStatusVO(message.getTaskId(), message.getUserId(), AiConstants.TASK_FAILED,
                            "任务执行失败: " + e.getMessage()),
                    TASK_TTL_DAYS, TimeUnit.DAYS);
            if (!exhausted) {
                // 未超限：重抛交由 RocketMQ 重试（成功结果会覆盖 FAILED 状态）
                throw e;
            }
            // 已超限：确认消费不再重投，避免持续调用大模型产生费用
        }
    }
}
