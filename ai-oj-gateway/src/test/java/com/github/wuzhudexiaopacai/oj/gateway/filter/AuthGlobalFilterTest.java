package com.github.wuzhudexiaopacai.oj.gateway.filter;

import com.github.wuzhudexiaopacai.oj.common.core.constant.CommonConstants;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.util.JwtUtils;
import com.github.wuzhudexiaopacai.oj.gateway.config.GatewaySecurityProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 网关认证过滤器测试：外部伪造身份头必须被剥离
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class AuthGlobalFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private GatewaySecurityProperties securityProperties;

    @Test
    void anonymousWhiteListRequestShouldStripForgedIdentityHeaders() {
        when(securityProperties.getWhiteList()).thenReturn(List.of("/api/contest/**"));
        AuthGlobalFilter filter = new AuthGlobalFilter(jwtUtils, redisTemplate, securityProperties);

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/contest")
                .header(CommonConstants.HEADER_USER_ID, "1")
                .header(CommonConstants.HEADER_USER_NAME, "admin")
                .header(CommonConstants.HEADER_AUTHORITIES, "contest:manage")
                .build();

        HttpHeaders headers = capture(filter, MockServerWebExchange.from(request)).getRequest().getHeaders();

        assertNull(headers.getFirst(CommonConstants.HEADER_USER_ID));
        assertNull(headers.getFirst(CommonConstants.HEADER_USER_NAME));
        assertNull(headers.getFirst(CommonConstants.HEADER_AUTHORITIES));
    }

    @Test
    void authenticatedRequestShouldIgnoreForgedIdentityHeaders() {
        when(securityProperties.getWhiteList()).thenReturn(List.of("/api/contest/**"));
        when(jwtUtils.parseToken("token")).thenReturn(LoginUser.builder()
                .id(9L).nickname("bob").authorities(List.of("user")).tokenId("jti").build());
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        AuthGlobalFilter filter = new AuthGlobalFilter(jwtUtils, redisTemplate, securityProperties);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/contest/1")
                .header(CommonConstants.TOKEN_HEADER, CommonConstants.TOKEN_PREFIX + "token")
                .header(CommonConstants.HEADER_USER_ID, "1")
                .header(CommonConstants.HEADER_AUTHORITIES, "contest:manage")
                .build();

        HttpHeaders headers = capture(filter, MockServerWebExchange.from(request)).getRequest().getHeaders();

        assertEquals("9", headers.getFirst(CommonConstants.HEADER_USER_ID));
        assertEquals("bob", headers.getFirst(CommonConstants.HEADER_USER_NAME));
        assertEquals("user", headers.getFirst(CommonConstants.HEADER_AUTHORITIES));
    }

    private ServerWebExchange capture(AuthGlobalFilter filter, ServerWebExchange exchange) {
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        filter.filter(exchange, ex -> {
            captured.set(ex);
            return Mono.empty();
        }).block();
        return captured.get();
    }
}
