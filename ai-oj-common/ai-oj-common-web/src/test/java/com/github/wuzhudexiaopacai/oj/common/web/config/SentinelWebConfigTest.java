package com.github.wuzhudexiaopacai.oj.common.web.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sentinel 限流响应测试：统一 Result 结构，前端可按 code=429 处理
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
class SentinelWebConfigTest {

    private final BlockExceptionHandler handler = new SentinelWebConfig().sentinelBlockExceptionHandler();

    @Test
    void blockShouldReturnUnifiedRateLimitResult() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, "/auth/login", new FlowException("/auth/login"));

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":429"));
        assertEquals("请求过于频繁，请稍后再试", response.getContentAsString().split("\"message\":\"")[1].split("\"")[0]);
    }
}
