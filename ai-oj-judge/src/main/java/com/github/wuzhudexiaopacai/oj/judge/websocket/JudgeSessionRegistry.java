package com.github.wuzhudexiaopacai.oj.judge.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本机 WebSocket 会话注册表（submitId → Session）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
public class JudgeSessionRegistry {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 注册会话
     */
    public void register(String submitId, WebSocketSession session) {
        sessions.put(submitId, session);
    }

    /**
     * 移除会话
     */
    public void remove(String submitId, WebSocketSession session) {
        sessions.remove(submitId, session);
    }

    /**
     * 获取本机会话
     */
    public WebSocketSession get(String submitId) {
        return sessions.get(submitId);
    }
}
