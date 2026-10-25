package com.github.wuzhudexiaopacai.oj.ai.service.impl;

import com.github.wuzhudexiaopacai.oj.ai.constant.AiConstants;
import com.github.wuzhudexiaopacai.oj.ai.dto.AiTaskMessage;
import com.github.wuzhudexiaopacai.oj.ai.dto.TaskSubmitDTO;
import com.github.wuzhudexiaopacai.oj.ai.service.AiTaskService;
import com.github.wuzhudexiaopacai.oj.ai.vo.TaskStatusVO;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * AI 异步任务服务实现（Redis 状态跟踪 + RocketMQ 异步执行）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskServiceImpl implements AiTaskService {

    /**
     * 任务结果 Redis TTL：1 天
     */
    private static final long TASK_TTL_DAYS = 1L;

    private final RedisTemplate<String, Object> redisTemplate;

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public String submitTask(TaskSubmitDTO req) {
        SecurityUtils.requireLogin();
        Map<String, String> params = buildParams(req);

        String taskId = UUID.randomUUID().toString().replace("-", "");
        String taskKey = AiConstants.AI_TASK_KEY + taskId;
        // 先落 PENDING 状态，再投递 MQ（避免消费快于状态写入）
        redisTemplate.opsForValue().set(taskKey,
                new TaskStatusVO(taskId, AiConstants.TASK_PENDING, null), TASK_TTL_DAYS, TimeUnit.DAYS);

        AiTaskMessage message = AiTaskMessage.builder()
                .taskId(taskId)
                .taskType(req.getTaskType())
                .params(params)
                .build();
        try {
            rocketMQTemplate.syncSend(MqConstants.AI_TASK_TOPIC,
                    MessageBuilder.withPayload(message)
                            .setHeader(RocketMQHeaders.KEYS, taskId)
                            .build());
        } catch (Exception e) {
            // 投递失败回滚状态，允许用户重新提交
            redisTemplate.delete(taskKey);
            log.error("AI 任务投递失败: taskId={}", taskId, e);
            throw e;
        }
        return taskId;
    }

    @Override
    public TaskStatusVO getTask(String taskId) {
        SecurityUtils.requireLogin();
        Object cached = redisTemplate.opsForValue().get(AiConstants.AI_TASK_KEY + taskId);
        Asserts.notNull(cached, "任务不存在或已过期");
        return (TaskStatusVO) cached;
    }

    /**
     * 按任务类型校验并提取模板变量
     */
    private Map<String, String> buildParams(TaskSubmitDTO req) {
        Map<String, String> params = new HashMap<>();
        if (AiConstants.TASK_TYPE_GENERATE.equals(req.getTaskType())) {
            Asserts.notBlank(req.getTopic(), "知识点 topic 不能为空");
            params.put("topic", req.getTopic());
            params.put("difficulty", req.getDifficulty() == null || req.getDifficulty().isBlank()
                    ? "中等" : req.getDifficulty());
        } else if (AiConstants.TASK_TYPE_REVIEW.equals(req.getTaskType())) {
            Asserts.notBlank(req.getLanguage(), "编程语言 language 不能为空");
            Asserts.notBlank(req.getCode(), "代码 code 不能为空");
            params.put("language", req.getLanguage());
            params.put("code", req.getCode());
        } else {
            Asserts.fail("不支持的任务类型: " + req.getTaskType());
        }
        return params;
    }
}
