package com.github.wuzhudexiaopacai.oj.judge.websocket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.util.JwtUtils;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 判题 WebSocket 握手拦截器
 * <p>
 * /ws/** 在网关白名单内，浏览器 WebSocket 又无法自定义 Authorization 头，
 * 因此在握手查询参数中接收 token，并在此校验 JWT、注销黑名单及 submitId 归属，
 * 防止仅凭他人 submitId 订阅判题结果。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class JudgeHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtils jwtUtils;

    private final StringRedisTemplate stringRedisTemplate;

    private final JudgeSubmissionMapper judgeSubmissionMapper;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String submitId = resolveQueryParam(request, "submitId");
        String token = resolveQueryParam(request, "token");
        if (!StringUtils.hasText(submitId) || !StringUtils.hasText(token)) {
            return reject(response, HttpStatus.UNAUTHORIZED);
        }

        LoginUser loginUser;
        try {
            loginUser = jwtUtils.parseToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            return reject(response, HttpStatus.UNAUTHORIZED);
        }
        if (loginUser.getId() == null || !StringUtils.hasText(loginUser.getTokenId())
                || Boolean.TRUE.equals(stringRedisTemplate.hasKey(
                        RedisKeys.TOKEN_BLACKLIST + loginUser.getTokenId()))) {
            return reject(response, HttpStatus.UNAUTHORIZED);
        }

        Long ownedCount = judgeSubmissionMapper.selectCount(new LambdaQueryWrapper<JudgeSubmission>()
                .eq(JudgeSubmission::getSubmitId, submitId)
                .eq(JudgeSubmission::getUserId, loginUser.getId()));
        if (ownedCount == null || ownedCount == 0) {
            return reject(response, HttpStatus.FORBIDDEN);
        }

        attributes.put(JudgeWebSocketHandler.ATTR_SUBMIT_ID, submitId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需后置处理
    }

    private String resolveQueryParam(ServerHttpRequest request, String name) {
        return UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst(name);
    }

    private boolean reject(ServerHttpResponse response, HttpStatus status) {
        response.setStatusCode(status);
        return false;
    }
}
