package com.github.wuzhudexiaopacai.oj.judge.sandbox;

import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSandbox;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSandboxMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 沙箱注册中心测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class SandboxRegistryTest {

    @Mock
    private JudgeSandboxMapper judgeSandboxMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @BeforeEach
    void setUpLock() throws InterruptedException {
        lenient().when(redissonClient.getLock(anyString())).thenReturn(lock);
        lenient().when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        lenient().when(lock.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    void refreshHeartbeatsShouldUpdateOnlineAndOffline() throws Exception {
        JudgeSandbox healthy = sandbox(1L, "sandbox-a", "http://healthy:8080");
        JudgeSandbox unhealthy = sandbox(2L, "sandbox-b", "http://broken:8080");
        when(judgeSandboxMapper.selectList(any())).thenReturn(List.of(healthy, unhealthy));

        newRegistry(baseUrl -> baseUrl.contains("healthy")).refreshHeartbeats();

        ArgumentCaptor<JudgeSandbox> captor = ArgumentCaptor.forClass(JudgeSandbox.class);
        verify(judgeSandboxMapper, org.mockito.Mockito.times(2)).updateById(captor.capture());
        JudgeSandbox online = captor.getAllValues().get(0);
        assertEquals(1L, online.getId());
        assertEquals(1, online.getStatus());
        assertNotNull(online.getLastHeartbeat());
        JudgeSandbox offline = captor.getAllValues().get(1);
        assertEquals(2L, offline.getId());
        assertEquals(0, offline.getStatus());
        assertNull(offline.getLastHeartbeat());
    }

    @Test
    void refreshHeartbeatsShouldDoNothingWhenNoSandbox() throws Exception {
        when(judgeSandboxMapper.selectList(any())).thenReturn(List.of());

        newRegistry(baseUrl -> true).refreshHeartbeats();

        verify(judgeSandboxMapper, never()).updateById(any(JudgeSandbox.class));
    }

    @Test
    void refreshHeartbeatsShouldSkipWhenAnotherInstanceHoldsLock() throws Exception {
        // 多实例部署：未抢到锁的实例本轮不探测、不写心跳
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        newRegistry(baseUrl -> true).refreshHeartbeats();

        verify(judgeSandboxMapper, never()).selectList(any());
        verify(lock, never()).unlock();
    }

    @Test
    void refreshHeartbeatsShouldUnlockAfterProbe() throws Exception {
        when(judgeSandboxMapper.selectList(any())).thenReturn(List.of(sandbox(1L, "sandbox-a", "http://a:8080")));

        newRegistry(baseUrl -> true).refreshHeartbeats();

        verify(lock).unlock();
    }

    @Test
    void listAvailableShouldReturnRegistryQueryResult() {
        JudgeSandbox sandbox = sandbox(1L, "sandbox-a", "http://healthy:8080");
        when(judgeSandboxMapper.selectList(any())).thenReturn(List.of(sandbox));

        List<JudgeSandbox> available = newRegistry(baseUrl -> true).listAvailable();

        assertEquals(1, available.size());
        assertEquals("sandbox-a", available.get(0).getSandboxKey());
    }

    @Test
    void isFreshShouldRejectStaleHeartbeat() {
        SandboxRegistry registry = newRegistry(baseUrl -> true);
        LocalDateTime now = LocalDateTime.now();

        JudgeSandbox noHeartbeat = sandbox(1L, "a", "http://a");
        JudgeSandbox recent = sandbox(2L, "b", "http://b");
        recent.setLastHeartbeat(now.minusSeconds(30));
        JudgeSandbox stale = sandbox(3L, "c", "http://c");
        stale.setLastHeartbeat(now.minusSeconds(91));

        assertTrue(registry.isFresh(noHeartbeat, now));
        assertTrue(registry.isFresh(recent, now));
        assertFalse(registry.isFresh(stale, now));
    }

    private SandboxRegistry newRegistry(SandboxProbe probe) {
        return new SandboxRegistry(judgeSandboxMapper, probe, redissonClient, 90);
    }

    private JudgeSandbox sandbox(Long id, String key, String baseUrl) {
        JudgeSandbox sandbox = new JudgeSandbox();
        sandbox.setId(id);
        sandbox.setSandboxKey(key);
        sandbox.setBaseUrl(baseUrl);
        sandbox.setStatus(1);
        return sandbox;
    }
}
