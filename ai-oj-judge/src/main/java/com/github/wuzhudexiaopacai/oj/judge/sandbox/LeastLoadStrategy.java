package com.github.wuzhudexiaopacai.oj.judge.sandbox;

import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSandbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/**
 * 最小负载优先策略：选择 当前负载/最大并行 比值最小的沙箱
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeastLoadStrategy implements SandboxLoadStrategy {

    /**
     * 沙箱拉黑时长（秒）
     */
    private static final long BLACKLIST_SECONDS = 30L;

    private static final String SANDBOX_DOWN_KEY = "judge:sandbox:down:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public JudgeSandbox select(List<JudgeSandbox> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .filter(s -> !Boolean.TRUE.equals(stringRedisTemplate.hasKey(SANDBOX_DOWN_KEY + s.getSandboxKey())))
                .min(Comparator.comparingDouble(this::loadRatio))
                .orElse(null);
    }

    @Override
    public void occupy(String sandboxKey) {
        stringRedisTemplate.opsForHash().increment(RedisKeys.SANDBOX_LOAD, sandboxKey, 1);
    }

    @Override
    public void release(String sandboxKey) {
        stringRedisTemplate.opsForHash().increment(RedisKeys.SANDBOX_LOAD, sandboxKey, -1);
    }

    @Override
    public void blacklist(String sandboxKey) {
        stringRedisTemplate.opsForValue().set(SANDBOX_DOWN_KEY + sandboxKey, "1", Duration.ofSeconds(BLACKLIST_SECONDS));
        log.warn("沙箱 {} 已被拉黑 {} 秒", sandboxKey, BLACKLIST_SECONDS);
    }

    /**
     * 负载率 = 当前任务数 / 最大并行数
     */
    private double loadRatio(JudgeSandbox sandbox) {
        Object value = stringRedisTemplate.opsForHash().get(RedisKeys.SANDBOX_LOAD, sandbox.getSandboxKey());
        long load = value == null ? 0L : Long.parseLong(value.toString());
        int maxParallel = sandbox.getMaxParallel() == null || sandbox.getMaxParallel() <= 0 ? 1 : sandbox.getMaxParallel();
        return (double) load / maxParallel;
    }
}
