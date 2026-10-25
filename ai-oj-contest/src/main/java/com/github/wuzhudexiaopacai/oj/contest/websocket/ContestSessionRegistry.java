package com.github.wuzhudexiaopacai.oj.contest.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本机比赛时间 WebSocket 会话注册表（contestId → 会话集合）
 * <p>
 * 与判题推送不同，比赛时间按比赛维度广播，同一场比赛可同时存在多个用户/多个标签页连接。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
public class ContestSessionRegistry {

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    /**
     * 注册会话
     */
    public void register(Long contestId, WebSocketSession session) {
        sessions.computeIfAbsent(contestId, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    /**
     * 移除会话（集合为空时一并清理比赛维度，避免内存泄漏）
     */
    public void remove(Long contestId, WebSocketSession session) {
        Set<WebSocketSession> set = sessions.get(contestId);
        if (set == null) {
            return;
        }
        set.remove(session);
        if (set.isEmpty()) {
            sessions.remove(contestId, set);
        }
    }

    /**
     * 获取某场比赛的本机会话集合
     */
    public Set<WebSocketSession> get(Long contestId) {
        Set<WebSocketSession> set = sessions.get(contestId);
        return set == null ? Collections.emptySet() : set;
    }
}
