package com.github.wuzhudexiaopacai.oj.contest.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 比赛时间 WebSocket 配置（原生 Spring WebSocket，非 STOMP）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class ContestWebSocketConfig implements WebSocketConfigurer {

    private final ContestWebSocketHandler contestWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(contestWebSocketHandler, "/ws/contest")
                .setAllowedOriginPatterns("*");
    }
}
