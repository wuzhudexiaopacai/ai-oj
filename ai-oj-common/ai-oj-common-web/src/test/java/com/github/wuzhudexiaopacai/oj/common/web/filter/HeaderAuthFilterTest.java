package com.github.wuzhudexiaopacai.oj.common.web.filter;

import com.github.wuzhudexiaopacai.oj.common.core.constant.CommonConstants;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 请求头认证过滤器测试：身份头必须携带正确的网关信任令牌才生效
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
class HeaderAuthFilterTest {

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/submission/1");

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private final MockFilterChain chain = new MockFilterChain();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateWhenGatewayTokenMatches() throws Exception {
        request.addHeader(CommonConstants.HEADER_USER_ID, "7");
        request.addHeader(CommonConstants.HEADER_AUTHORITIES, "USER");
        request.addHeader(CommonConstants.HEADER_GATEWAY_TOKEN, "trusted-token");

        new HeaderAuthFilter("trusted-token").doFilter(request, response, chain);

        assertEquals(7L, currentUser().getId());
        assertEquals(1, SecurityContextHolder.getContext().getAuthentication().getAuthorities().size());
    }

    @Test
    void shouldIgnoreIdentityHeadersWhenGatewayTokenMismatches() throws Exception {
        request.addHeader(CommonConstants.HEADER_USER_ID, "7");
        request.addHeader(CommonConstants.HEADER_GATEWAY_TOKEN, "forged-token");

        new HeaderAuthFilter("trusted-token").doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldAcceptIdentityHeadersWhenGatewayTokenNotConfigured() throws Exception {
        request.addHeader(CommonConstants.HEADER_USER_ID, "7");

        new HeaderAuthFilter("").doFilter(request, response, chain);

        assertEquals(7L, currentUser().getId());
    }

    private LoginUser currentUser() {
        return (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
