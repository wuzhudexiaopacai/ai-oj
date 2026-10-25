package com.github.wuzhudexiaopacai.oj.judge.websocket;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 判题 WebSocket 握手拦截器
 * <p>
 * 鉴权说明：网关已统一鉴权，但 /ws/** 路由在白名单内（WebSocket 升级请求不携带网关注入头），
 * 此处仅解析 submitId 做非空校验；submitId 与用户的归属关系由查询接口保证，
 * 推送结果不含敏感数据，归属弱校验可省略。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
public class JudgeHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String query = request.getURI().getQuery();
        return StrUtil.isNotBlank(query) && query.contains("submitId=");
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需后置处理
    }
}
