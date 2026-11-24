package com.github.wuzhudexiaopacai.oj.gateway.filter;

import com.github.wuzhudexiaopacai.oj.common.core.constant.CommonConstants;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.util.JwtUtils;
import com.github.wuzhudexiaopacai.oj.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * 全局认证过滤器
 * <p>
 * 职责：
 * 1. 剥离外部伪造的内部调用头（X-Internal-Call），防止越权绕过网关；
 * 2. 白名单直接放行；
 * 3. 解析 JWT 并校验 Token 黑名单（注销登出场景）；
 * 4. 向下游注入 X-User-Id / X-User-Name / X-User-Authorities 三个受信头。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;

    private final ReactiveStringRedisTemplate redisTemplate;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final GatewaySecurityProperties securityProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 链路 ID：透传上游或新生成，供下游运行日志与操作日志串联
        String traceHeader = request.getHeaders().getFirst(CommonConstants.HEADER_TRACE_ID);
        String traceId = StringUtils.hasText(traceHeader)
                ? traceHeader
                : UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 无论是否白名单，外部请求一律剥离内部调用头、身份头与网关令牌，防止伪造内网身份
        // 身份头只允许由本过滤器在 JWT 校验通过后注入；白名单匿名请求若不剥离，可伪造任意用户与权限
        String gatewayToken = securityProperties.getGatewayToken();
        ServerHttpRequest sanitized = request.mutate()
                .headers(headers -> {
                    headers.remove(CommonConstants.HEADER_INTERNAL_CALL);
                    headers.remove(CommonConstants.HEADER_USER_ID);
                    headers.remove(CommonConstants.HEADER_USER_NAME);
                    headers.remove(CommonConstants.HEADER_AUTHORITIES);
                    headers.remove(CommonConstants.HEADER_GATEWAY_TOKEN);
                    headers.set(CommonConstants.HEADER_TRACE_ID, traceId);
                    // 注入信任令牌：下游服务据此确认身份头确实来自网关，直连业务端口无法伪造
                    if (StringUtils.hasText(gatewayToken)) {
                        headers.set(CommonConstants.HEADER_GATEWAY_TOKEN, gatewayToken);
                    }
                })
                .build();

        // 白名单路径：无 token 直接放行为匿名；带 token 时仍解析身份，
        // 否则白名单中的写接口（如比赛报名）无法识别当前登录用户
        boolean white = isWhite(path);

        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return white
                    ? chain.filter(exchange.mutate().request(sanitized).build())
                    : unauthorized(exchange.getResponse(), "未携带认证令牌");
        }

        LoginUser loginUser;
        try {
            loginUser = jwtUtils.parseToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token 解析失败: {}", e.getMessage());
            // 白名单路径即便 token 无效也按匿名放行，避免公开内容被一枚坏 token 挡住
            return white
                    ? chain.filter(exchange.mutate().request(sanitized).build())
                    : unauthorized(exchange.getResponse(), "令牌无效或已过期");
        }

        // 黑名单校验（注销/封禁 Token 立即失效）
        return redisTemplate.hasKey(RedisKeys.TOKEN_BLACKLIST + loginUser.getTokenId())
                .defaultIfEmpty(Boolean.FALSE)
                .flatMap(blocked -> {
                    if (Boolean.TRUE.equals(blocked)) {
                        return white
                                ? chain.filter(exchange.mutate().request(sanitized).build())
                                : unauthorized(exchange.getResponse(), "令牌已注销");
                    }
                    ServerHttpRequest authed = sanitized.mutate()
                            .header(CommonConstants.HEADER_USER_ID, String.valueOf(loginUser.getId()))
                            .header(CommonConstants.HEADER_USER_NAME,
                                    URLEncoder.encode(loginUser.getNickname() == null ? "" : loginUser.getNickname(),
                                            StandardCharsets.UTF_8))
                            .header(CommonConstants.HEADER_AUTHORITIES,
                                    String.join(",", loginUser.getAuthorities()))
                            .build();
                    return chain.filter(exchange.mutate().request(authed).build());
                });
    }

    /**
     * 白名单匹配（Ant 风格路径）
     */
    private boolean isWhite(String path) {
        List<String> whiteList = securityProperties.getWhiteList();
        if (whiteList == null || whiteList.isEmpty()) {
            return false;
        }
        return whiteList.stream().anyMatch(pattern -> pathMatcher.match(pattern.trim(), path));
    }

    private String resolveToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(CommonConstants.TOKEN_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return header.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 在网关内置过滤器之前执行
        return -100;
    }
}
