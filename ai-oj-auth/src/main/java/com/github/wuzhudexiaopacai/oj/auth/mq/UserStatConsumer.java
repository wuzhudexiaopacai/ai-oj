package com.github.wuzhudexiaopacai.oj.auth.mq;

import com.github.wuzhudexiaopacai.oj.api.dto.JudgeResultMessage;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysUserMapper;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 用户统计消费者（订阅判题结果，累加 sys_user.submit_count / accept_count）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = MqConstants.JUDGE_RESULT_TOPIC, consumerGroup = MqConstants.GROUP_USER_STAT)
public class UserStatConsumer implements RocketMQListener<JudgeResultMessage> {

    /**
     * 统计幂等标记前缀（+ submitId，TTL 7 天）
     */
    private static final String STAT_IDEM_KEY = "auth:user:stat:idem:";

    private final SysUserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(JudgeResultMessage message) {
        if (message == null || message.getSubmitId() == null || message.getUserId() == null) {
            log.warn("判题结果消息字段缺失，直接消费成功: {}", message);
            return;
        }
        // 幂等：同一提交只统计一次，重复消息直接消费成功
        String idemKey = STAT_IDEM_KEY + message.getSubmitId();
        Boolean first = redisTemplate.opsForValue().setIfAbsent(idemKey, "1", 7, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(first)) {
            log.info("判题结果消息重复，跳过统计: submitId={}", message.getSubmitId());
            return;
        }
        try {
            int acceptDelta = Objects.equals(message.getStatusCode(), JudgeStatusEnum.ACCEPTED.getCode()) ? 1 : 0;
            userMapper.incrementStat(message.getUserId(), acceptDelta);
            // 用户信息（含统计字段）带缓存，更新后失效，避免个人中心展示旧数据
            redisTemplate.delete(RedisKeys.USER_INFO + message.getUserId());
        } catch (Exception e) {
            // 处理失败删除幂等标记，让消息重试时重新消费
            redisTemplate.delete(idemKey);
            log.error("用户统计失败: {}", message, e);
            throw e;
        }
    }
}
