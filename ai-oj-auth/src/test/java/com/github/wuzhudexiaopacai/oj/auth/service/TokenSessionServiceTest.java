package com.github.wuzhudexiaopacai.oj.auth.service;

import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户令牌会话管理测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class TokenSessionServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenSessionService tokenSessionService;

    @BeforeEach
    void setUp() {
        tokenSessionService = new TokenSessionService(stringRedisTemplate);
        ReflectionTestUtils.setField(tokenSessionService, "expireMinutes", 720L);
    }

    @Test
    void registerShouldAddJtiAndRefreshExpire() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);

        tokenSessionService.register(1L, "jti-1");

        String key = RedisKeys.USER_TOKENS + 1L;
        verify(setOperations).add(key, "jti-1");
        verify(stringRedisTemplate).expire(key, 720L, TimeUnit.MINUTES);
    }

    @Test
    void revokeShouldRemoveFromIndexAndBlacklist() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenSessionService.revoke(1L, "jti-1");

        verify(setOperations).remove(RedisKeys.USER_TOKENS + 1L, "jti-1");
        verify(valueOperations).set(RedisKeys.TOKEN_BLACKLIST + "jti-1", "1", 720L, TimeUnit.MINUTES);
    }

    @Test
    void revokeAllShouldBlacklistEveryTokenAndDeleteIndex() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(setOperations.members(RedisKeys.USER_TOKENS + 1L)).thenReturn(Set.of("j1", "j2"));

        tokenSessionService.revokeAll(1L);

        verify(valueOperations).set(eq(RedisKeys.TOKEN_BLACKLIST + "j1"), eq("1"), anyLong(), eq(TimeUnit.MINUTES));
        verify(valueOperations).set(eq(RedisKeys.TOKEN_BLACKLIST + "j2"), eq("1"), anyLong(), eq(TimeUnit.MINUTES));
        verify(stringRedisTemplate).delete(RedisKeys.USER_TOKENS + 1L);
    }
}
