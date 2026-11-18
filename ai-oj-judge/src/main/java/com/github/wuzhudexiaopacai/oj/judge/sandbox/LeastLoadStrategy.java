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

    /**
     * 负载计数 TTL：远大于单次判题上限（120s），判题进程崩溃未 release 时计数也能自动归零，
     * 避免某个实例被永久标记为高负载
     */
    private static final Duration LOAD_TTL = Duration.ofMinutes(10);

    private static final String SANDBOX_DOWN_KEY = "judge:sandbox:down:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public JudgeSandbox select(List<JudgeSandbox> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        // 仅在健康实例中选择；全部实例都达到并行上限时仍返回负载最低者，
        // 交由沙箱自身排队（信号量）处理，而不是让判题直接失败
        return candidates.stream()
                .filter(s -> !Boolean.TRUE.equals(stringRedisTemplate.hasKey(SANDBOX_DOWN_KEY + s.getSandboxKey())))
                .min(Comparator.comparingDouble(this::loadRatio))
                .orElse(null);
    }

    @Override
    public void occupy(String sandboxKey) {
        String key = RedisKeys.SANDBOX_LOAD + sandboxKey;
        Long load = stringRedisTemplate.opsForValue().increment(key);
        // 每次占用都刷新 TTL，保证 TTL 只在整个沙箱空闲一段时间后才生效
        if (load != null) {
            stringRedisTemplate.expire(key, LOAD_TTL);
        }
    }

    @Override
    public void release(String sandboxKey) {
        String key = RedisKeys.SANDBOX_LOAD + sandboxKey;
        Long load = stringRedisTemplate.opsForValue().decrement(key);
        // 减到 0 及以下说明已无任务，直接删除计数，避免负数残留
        if (load != null && load <= 0) {
            stringRedisTemplate.delete(key);
        }
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
        String value = stringRedisTemplate.opsForValue().get(RedisKeys.SANDBOX_LOAD + sandbox.getSandboxKey());
        long load = value == null ? 0L : Long.parseLong(value);
        int maxParallel = sandbox.getMaxParallel() == null || sandbox.getMaxParallel() <= 0 ? 1 : sandbox.getMaxParallel();
        return (double) load / maxParallel;
    }
}
