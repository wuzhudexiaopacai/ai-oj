package com.github.wuzhudexiaopacai.oj.judge.sandbox;

import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSandbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 最小负载策略测试：计数带 TTL、减到 0 删除、超限仍可选机
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class LeastLoadStrategyTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LeastLoadStrategy leastLoadStrategy;

    @Test
    void occupyShouldIncrementAndRefreshTtl() {
        String key = RedisKeys.SANDBOX_LOAD + "sandbox-a";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(1L);

        leastLoadStrategy.occupy("sandbox-a");

        verify(stringRedisTemplate).expire(key, Duration.ofMinutes(10));
    }

    @Test
    void releaseShouldDeleteCounterWhenNoTaskLeft() {
        String key = RedisKeys.SANDBOX_LOAD + "sandbox-a";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement(key)).thenReturn(0L);

        leastLoadStrategy.release("sandbox-a");

        verify(stringRedisTemplate).delete(key);
    }

    @Test
    void selectShouldSkipBlacklistedAndPickLowestLoad() {
        JudgeSandbox down = sandboxOf("sandbox-a", 2);
        JudgeSandbox idle = sandboxOf("sandbox-b", 4);
        JudgeSandbox busy = sandboxOf("sandbox-c", 2);
        when(stringRedisTemplate.hasKey("judge:sandbox:down:sandbox-a")).thenReturn(true);
        when(stringRedisTemplate.hasKey("judge:sandbox:down:sandbox-b")).thenReturn(false);
        when(stringRedisTemplate.hasKey("judge:sandbox:down:sandbox-c")).thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.SANDBOX_LOAD + "sandbox-b")).thenReturn("2");
        when(valueOperations.get(RedisKeys.SANDBOX_LOAD + "sandbox-c")).thenReturn("3");

        JudgeSandbox selected = leastLoadStrategy.select(List.of(down, idle, busy));

        // b: 2/4 = 0.5，c: 3/2 = 1.5，a 已拉黑
        assertEquals("sandbox-b", selected.getSandboxKey());
    }

    @Test
    void selectShouldStillReturnLeastLoadedWhenAllOverloaded() {
        JudgeSandbox first = sandboxOf("sandbox-a", 1);
        JudgeSandbox second = sandboxOf("sandbox-b", 1);
        when(stringRedisTemplate.hasKey("judge:sandbox:down:sandbox-a")).thenReturn(false);
        when(stringRedisTemplate.hasKey("judge:sandbox:down:sandbox-b")).thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.SANDBOX_LOAD + "sandbox-a")).thenReturn("5");
        when(valueOperations.get(RedisKeys.SANDBOX_LOAD + "sandbox-b")).thenReturn("2");

        JudgeSandbox selected = leastLoadStrategy.select(List.of(first, second));

        // 全部超限时交给沙箱排队，而不是直接返回 null 让判题失败
        assertEquals("sandbox-b", selected.getSandboxKey());
    }

    @Test
    void selectShouldReturnNullForEmptyCandidates() {
        assertNull(leastLoadStrategy.select(List.of()));
        assertNull(leastLoadStrategy.select(null));
    }

    private JudgeSandbox sandboxOf(String key, int maxParallel) {
        JudgeSandbox sandbox = new JudgeSandbox();
        sandbox.setSandboxKey(key);
        sandbox.setMaxParallel(maxParallel);
        return sandbox;
    }
}
