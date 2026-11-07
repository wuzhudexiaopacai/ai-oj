package com.github.wuzhudexiaopacai.oj.judge.sandbox;

import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSandbox;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSandboxMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void refreshHeartbeatsShouldUpdateOnlineAndOffline() {
        JudgeSandbox healthy = sandbox(1L, "sandbox-a", "http://healthy:8080");
        JudgeSandbox unhealthy = sandbox(2L, "sandbox-b", "http://broken:8080");
        when(judgeSandboxMapper.selectList(any())).thenReturn(List.of(healthy, unhealthy));

        SandboxRegistry registry = new SandboxRegistry(judgeSandboxMapper,
                baseUrl -> baseUrl.contains("healthy"), 90);
        registry.refreshHeartbeats();

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
    void refreshHeartbeatsShouldDoNothingWhenNoSandbox() {
        when(judgeSandboxMapper.selectList(any())).thenReturn(List.of());

        new SandboxRegistry(judgeSandboxMapper, baseUrl -> true, 90).refreshHeartbeats();

        verify(judgeSandboxMapper, never()).updateById(any(JudgeSandbox.class));
    }

    @Test
    void listAvailableShouldReturnRegistryQueryResult() {
        JudgeSandbox sandbox = sandbox(1L, "sandbox-a", "http://healthy:8080");
        when(judgeSandboxMapper.selectList(any())).thenReturn(List.of(sandbox));

        List<JudgeSandbox> available = new SandboxRegistry(judgeSandboxMapper,
                baseUrl -> true, 90).listAvailable();

        assertEquals(1, available.size());
        assertEquals("sandbox-a", available.get(0).getSandboxKey());
    }

    @Test
    void isFreshShouldRejectStaleHeartbeat() {
        SandboxRegistry registry = new SandboxRegistry(judgeSandboxMapper, baseUrl -> true, 90);
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

    private JudgeSandbox sandbox(Long id, String key, String baseUrl) {
        JudgeSandbox sandbox = new JudgeSandbox();
        sandbox.setId(id);
        sandbox.setSandboxKey(key);
        sandbox.setBaseUrl(baseUrl);
        sandbox.setStatus(1);
        return sandbox;
    }
}
