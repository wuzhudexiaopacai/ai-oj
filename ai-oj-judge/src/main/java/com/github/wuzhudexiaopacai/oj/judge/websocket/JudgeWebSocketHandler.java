package com.github.wuzhudexiaopacai.oj.judge.websocket;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 判题进度 WebSocket 处理器（端点 /ws/judge，查询参数 submitId）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeWebSocketHandler extends TextWebSocketHandler {

    /**
     * submitId 会话属性名
     */
    public static final String ATTR_SUBMIT_ID = "submitId";

    private final JudgeSessionRegistry sessionRegistry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String submitId = resolveSubmitId(session);
        if (StrUtil.isBlank(submitId)) {
            closeQuietly(session);
            return;
        }
        session.getAttributes().put(ATTR_SUBMIT_ID, submitId);
        sessionRegistry.register(submitId, session);
        log.info("WebSocket 建立连接 submitId={}", submitId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object submitId = session.getAttributes().get(ATTR_SUBMIT_ID);
        if (submitId != null) {
            sessionRegistry.remove(submitId.toString(), session);
            log.info("WebSocket 连接关闭 submitId={}", submitId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket 传输异常", exception);
        closeQuietly(session);
    }

    private String resolveSubmitId(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }
        return UriComponentsBuilder.fromUri(session.getUri()).build()
                .getQueryParams().getFirst("submitId");
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close();
        } catch (Exception ignored) {
            // 关闭失败忽略
        }
    }
}
