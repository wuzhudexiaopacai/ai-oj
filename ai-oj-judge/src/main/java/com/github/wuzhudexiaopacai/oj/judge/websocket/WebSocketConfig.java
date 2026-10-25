package com.github.wuzhudexiaopacai.oj.judge.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置（原生 Spring WebSocket，非 STOMP）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final JudgeWebSocketHandler judgeWebSocketHandler;

    private final JudgeHandshakeInterceptor judgeHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(judgeWebSocketHandler, "/ws/judge")
                .addInterceptors(judgeHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
