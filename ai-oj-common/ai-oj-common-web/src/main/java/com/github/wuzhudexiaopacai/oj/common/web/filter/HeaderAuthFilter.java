package com.github.wuzhudexiaopacai.oj.common.web.filter;

import cn.hutool.core.util.StrUtil;
import com.github.wuzhudexiaopacai.oj.common.core.constant.CommonConstants;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public class HeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userId = request.getHeader(CommonConstants.HEADER_USER_ID);
        if (StrUtil.isNotBlank(userId)) {
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
}
