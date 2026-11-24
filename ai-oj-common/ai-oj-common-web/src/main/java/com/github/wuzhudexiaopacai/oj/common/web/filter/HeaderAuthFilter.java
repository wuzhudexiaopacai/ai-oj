package com.github.wuzhudexiaopacai.oj.common.web.filter;

import cn.hutool.core.util.StrUtil;
import com.github.wuzhudexiaopacai.oj.common.core.constant.CommonConstants;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 请求头认证过滤器：解析网关注入的用户请求头，构建 SecurityContext
 * <p>
 * 服务间 Feign 调用会透传这些头，保证下游服务也能拿到登录态。
 * 配置了网关令牌（{@code ai-oj.security.gateway-token}）时，只有携带正确令牌的请求才会采信身份头，
 * 防止业务端口被直连后伪造 {@code X-User-Id} 越权。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
public class HeaderAuthFilter extends OncePerRequestFilter {

    /**
     * 期望的网关信任令牌，为空表示不校验（仅限本地调试）
     */
    private final String expectedGatewayToken;

    public HeaderAuthFilter(String expectedGatewayToken) {
        this.expectedGatewayToken = expectedGatewayToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userId = request.getHeader(CommonConstants.HEADER_USER_ID);
        if (StrUtil.isNotBlank(userId)) {
            if (!trustedGatewayToken(request)) {
                log.warn("身份头未携带有效网关令牌，按匿名请求处理: uri={}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }
            String authorities = request.getHeader(CommonConstants.HEADER_AUTHORITIES);
            List<SimpleGrantedAuthority> authorityList = StrUtil.isBlank(authorities)
                    ? List.of()
                    : Arrays.stream(authorities.split(",")).map(SimpleGrantedAuthority::new).toList();
            String nickname = request.getHeader(CommonConstants.HEADER_USER_NAME);
            LoginUser loginUser = LoginUser.builder()
                    .id(Long.valueOf(userId))
                    .username(nickname == null ? "" : URLDecoder.decode(nickname, StandardCharsets.UTF_8))
                    .nickname(nickname == null ? "" : URLDecoder.decode(nickname, StandardCharsets.UTF_8))
                    .authorities(StrUtil.isBlank(authorities) ? List.of() : Arrays.asList(authorities.split(",")))
                    .build();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, authorityList);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 身份头是否可信：未配置令牌时直接放行，配置后必须与网关注入值一致
     */
    private boolean trustedGatewayToken(HttpServletRequest request) {
        if (StrUtil.isBlank(expectedGatewayToken)) {
            return true;
        }
        return expectedGatewayToken.equals(request.getHeader(CommonConstants.HEADER_GATEWAY_TOKEN));
    }
}
