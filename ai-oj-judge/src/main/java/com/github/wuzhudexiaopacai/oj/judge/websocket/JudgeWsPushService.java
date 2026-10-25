package com.github.wuzhudexiaopacai.oj.judge.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.judge.vo.JudgeProgressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 判题结果 WebSocket 推送服务
 * <p>
 * 多实例路由：判题完成时先尝试推本机会话；同时向 Redis 频道 "judge:ws:push" publish，
 * 所有实例订阅该频道，收到后转发本机会话，保证连接落在任意实例都能收到。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeWsPushService {

    /**
     * Redis pub/sub 频道
     */
    public static final String WS_PUSH_CHANNEL = "judge:ws:push";

    private final JudgeSessionRegistry sessionRegistry;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 推送判题结果（本机直推 + Redis 广播）
     *
     * @param progress 进度
     */
    public void push(JudgeProgressVO progress) {
        // 本机直推
        pushLocal(progress.getSubmitId(), progress);
        // 跨实例广播
        try {
            stringRedisTemplate.convertAndSend(WS_PUSH_CHANNEL, objectMapper.writeValueAsString(progress));
        } catch (Exception e) {
            log.error("判题结果 Redis 广播失败 submitId={}", progress.getSubmitId(), e);
        }
    }

    /**
     * 推送到本机会话（推送后关闭连接）
     *
     * @param submitId 幂等提交标识
     * @param progress 进度
     * @return true 本机有会话且推送成功
     */
    public boolean pushLocal(String submitId, JudgeProgressVO progress) {
        WebSocketSession session = sessionRegistry.get(submitId);
        if (session == null) {
            return false;
        }
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(progress)));
            }
            session.close();
            return true;
        } catch (Exception e) {
            log.warn("WebSocket 推送失败 submitId={}", submitId, e);
            return false;
        } finally {
            sessionRegistry.remove(submitId, session);
        }
    }
}
