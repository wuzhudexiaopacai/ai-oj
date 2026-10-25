package com.github.wuzhudexiaopacai.oj.contest.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.contest.vo.ContestTimeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

/**
 * 比赛时间 WebSocket 推送服务
 * <p>
 * 多实例路由：向 Redis 频道 "contest:ws:push" 广播，所有实例订阅后转发本机会话，
 * 保证用户连接落在任意实例都能收到时间变更；Redis 不可用时降级为本机直推。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContestWsPushService {

    /**
     * Redis pub/sub 频道
     */
    public static final String WS_PUSH_CHANNEL = "contest:ws:push";

    private final ContestSessionRegistry sessionRegistry;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 广播比赛时间快照（跨实例）
     *
     * @param timeVO 时间快照
     */
    public void publish(ContestTimeVO timeVO) {
        if (timeVO == null || timeVO.getContestId() == null) {
            return;
        }
        try {
            // 本实例同样订阅该频道，无需额外直推，避免重复下发
            stringRedisTemplate.convertAndSend(WS_PUSH_CHANNEL, objectMapper.writeValueAsString(timeVO));
        } catch (Exception e) {
            log.error("比赛时间 Redis 广播失败，降级为本机推送 contestId={}", timeVO.getContestId(), e);
            pushLocal(timeVO);
        }
    }

    /**
     * 推送到本机该比赛的全部会话
     *
     * @param timeVO 时间快照
     */
    public void pushLocal(ContestTimeVO timeVO) {
        if (timeVO == null || timeVO.getContestId() == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionRegistry.get(timeVO.getContestId());
        for (WebSocketSession session : sessions) {
            sendTo(timeVO.getContestId(), session, timeVO);
        }
    }

    /**
     * 推送到单个会话（连接建立时下发初始快照）
     *
     * @param contestId 比赛 ID
     * @param session   会话
     * @param timeVO    时间快照
     */
    public void sendTo(Long contestId, WebSocketSession session, ContestTimeVO timeVO) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(timeVO)));
        } catch (Exception e) {
            log.warn("比赛时间 WebSocket 推送失败 contestId={}", contestId, e);
            sessionRegistry.remove(contestId, session);
            try {
                session.close();
            } catch (Exception ignored) {
                // 关闭失败忽略
            }
        }
    }
}
