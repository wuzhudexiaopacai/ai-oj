package com.github.wuzhudexiaopacai.oj.auth.mq;

import com.github.wuzhudexiaopacai.oj.api.dto.JudgeResultMessage;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysUserMapper;
import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户统计消费者测试：幂等、失败重试与缓存失效隔离
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class UserStatConsumerTest {

    private static final String IDEM_KEY = "auth:user:stat:idem:7:submit-1";

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private UserStatConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new UserStatConsumer(userMapper, redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.delete(anyString())).thenReturn(true);
    }

    @Test
    void duplicateMessageShouldSkipStatistics() {
        when(valueOperations.setIfAbsent(eq(IDEM_KEY), any(), eq(7L), eq(TimeUnit.DAYS))).thenReturn(false);

        consumer.onMessage(acceptedMessage());

        verify(userMapper, never()).incrementStat(anyLong(), anyInt());
    }

    @Test
    void cacheEvictFailureShouldNotRetryMessage() {
        when(valueOperations.setIfAbsent(eq(IDEM_KEY), any(), eq(7L), eq(TimeUnit.DAYS))).thenReturn(true);
        when(redisTemplate.delete(eq("user:info:7"))).thenThrow(new RuntimeException("redis down"));

        assertDoesNotThrow(() -> consumer.onMessage(acceptedMessage()));

        // 统计已成功落库，缓存失效失败不能重放消息（否则提交数/通过数会重复累加）
        verify(userMapper).incrementStat(7L, 1);
        verify(redisTemplate, never()).delete(IDEM_KEY);
    }

    @Test
    void statFailureShouldClearIdempotentFlagAndRethrow() {
        when(valueOperations.setIfAbsent(eq(IDEM_KEY), any(), eq(7L), eq(TimeUnit.DAYS))).thenReturn(true);
        doThrow(new RuntimeException("db down")).when(userMapper).incrementStat(anyLong(), anyInt());

        assertThrows(RuntimeException.class, () -> consumer.onMessage(acceptedMessage()));

        // 失败必须清除幂等标记，让 MQ 重试时能重新统计
        verify(redisTemplate).delete(IDEM_KEY);
    }

    private JudgeResultMessage acceptedMessage() {
        return JudgeResultMessage.builder()
                .submitId("submit-1")
                .userId(7L)
                .problemId(100L)
                .contestId(0L)
                .statusCode(JudgeStatusEnum.ACCEPTED.getCode())
                .score(100)
                .build();
    }
}
