package com.github.wuzhudexiaopacai.oj.ai.service;

import com.github.wuzhudexiaopacai.oj.ai.dto.ReviewRequestDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话服务（流式对话 + 同步代码评审）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface AiChatService {

    /**
     * 流式对话（SSE，Redisson 限流 + 会话记忆 + 场景策略）
     *
     * @param sessionId 会话 ID
     * @param question  用户问题
     * @return SSE 发射器
     */
    SseEmitter streamChat(Long sessionId, String question);

    /**
     * 同步代码评审
     *
     * @param req 评审请求
     * @return 评审结果
     */
    String review(ReviewRequestDTO req);
}
