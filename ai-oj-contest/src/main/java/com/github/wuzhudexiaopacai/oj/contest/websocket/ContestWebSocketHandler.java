package com.github.wuzhudexiaopacai.oj.contest.websocket;

import cn.hutool.core.util.StrUtil;
import com.github.wuzhudexiaopacai.oj.contest.service.ContestService;
import com.github.wuzhudexiaopacai.oj.contest.vo.ContestTimeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 比赛时间 WebSocket 处理器（端点 /ws/contest，查询参数 contestId）
 * <p>
 * 连接建立后立即下发一次当前时间快照，后续仅在管理员调整比赛时间/状态迁移时推送。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContestWebSocketHandler extends TextWebSocketHandler {

    /**
     * contestId 会话属性名
     */
    public static final String ATTR_CONTEST_ID = "contestId";

    private final ContestSessionRegistry sessionRegistry;

    private final ContestWsPushService wsPushService;

    private final ContestService contestService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long contestId = resolveContestId(session);
        if (contestId == null) {
            closeQuietly(session);
            return;
        }
        ContestTimeVO snapshot = contestService.timeSnapshot(contestId);
        if (snapshot == null) {
            log.warn("比赛不存在，拒绝 WebSocket 连接 contestId={}", contestId);
            closeQuietly(session);
            return;
        }
        session.getAttributes().put(ATTR_CONTEST_ID, contestId);
        sessionRegistry.register(contestId, session);
        log.info("比赛时间 WebSocket 建立连接 contestId={}", contestId);
        wsPushService.sendTo(contestId, session, snapshot);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object contestId = session.getAttributes().get(ATTR_CONTEST_ID);
        if (contestId != null) {
            sessionRegistry.remove((Long) contestId, session);
            log.info("比赛时间 WebSocket 连接关闭 contestId={}", contestId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("比赛时间 WebSocket 传输异常", exception);
        closeQuietly(session);
    }

    /**
     * 从连接 URI 解析 contestId 查询参数
     */
    private Long resolveContestId(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }
        String raw = UriComponentsBuilder.fromUri(session.getUri()).build()
                .getQueryParams().getFirst("contestId");
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close();
        } catch (Exception ignored) {
            // 关闭失败忽略
        }
    }
}
