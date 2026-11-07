package com.github.wuzhudexiaopacai.oj.judge.sandbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSandbox;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSandboxMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 沙箱注册中心：定时健康探测刷新 last_heartbeat/status，选机时过滤心跳过期节点
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
public class SandboxRegistry {

    private final JudgeSandboxMapper judgeSandboxMapper;

    private final SandboxProbe sandboxProbe;

    /**
     * 心跳有效期（秒）：超过该时长未刷新视为过期
     */
    private final long staleSeconds;

    public SandboxRegistry(JudgeSandboxMapper judgeSandboxMapper,
                           SandboxProbe sandboxProbe,
                           @Value("${ai-oj.judge.sandbox.stale-seconds:90}") long staleSeconds) {
        this.judgeSandboxMapper = judgeSandboxMapper;
        this.sandboxProbe = sandboxProbe;
        this.staleSeconds = staleSeconds;
    }

    /**
     * 定时探测所有沙箱：健康则刷新心跳并置在线，不可用则置离线
     */
    @Scheduled(fixedDelayString = "${ai-oj.judge.sandbox.heartbeat-interval-ms:30000}")
    public void refreshHeartbeats() {
        List<JudgeSandbox> sandboxes = judgeSandboxMapper.selectList(null);
        if (sandboxes == null || sandboxes.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (JudgeSandbox sandbox : sandboxes) {
            boolean healthy = sandboxProbe.healthy(sandbox.getBaseUrl());
            JudgeSandbox update = new JudgeSandbox();
            update.setId(sandbox.getId());
            update.setStatus(healthy ? 1 : 0);
            if (healthy) {
                update.setLastHeartbeat(now);
            }
            judgeSandboxMapper.updateById(update);
            if (healthy != Integer.valueOf(1).equals(sandbox.getStatus())) {
                log.info("沙箱状态变更 sandbox={} status={}", sandbox.getSandboxKey(), update.getStatus());
            }
        }
    }

    /**
     * 查询可调度沙箱：在线且心跳未过期（心跳为空的历史数据视为可用，由下一轮探测刷新）
     *
     * @return 可用沙箱列表
     */
    public List<JudgeSandbox> listAvailable() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(staleSeconds);
        return judgeSandboxMapper.selectList(new LambdaQueryWrapper<JudgeSandbox>()
                .eq(JudgeSandbox::getStatus, 1)
                .and(w -> w.isNull(JudgeSandbox::getLastHeartbeat)
                        .or()
                        .ge(JudgeSandbox::getLastHeartbeat, threshold)));
    }

    /**
     * 心跳是否新鲜
     *
     * @param sandbox 沙箱
     * @param now     当前时间
     * @return true 未过期
     */
    boolean isFresh(JudgeSandbox sandbox, LocalDateTime now) {
        return sandbox.getLastHeartbeat() == null
                || sandbox.getLastHeartbeat().isAfter(now.minusSeconds(staleSeconds));
    }
}
