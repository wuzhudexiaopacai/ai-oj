package com.github.wuzhudexiaopacai.oj.ai.service;

import com.github.wuzhudexiaopacai.oj.ai.dto.SessionCreateDTO;
import com.github.wuzhudexiaopacai.oj.ai.entity.AiSession;
import com.github.wuzhudexiaopacai.oj.ai.vo.MessageVO;
import com.github.wuzhudexiaopacai.oj.ai.vo.SessionVO;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;

import java.util.List;

/**
 * AI 会话服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface AiSessionService {

    /**
     * 创建会话
     *
     * @param dto 创建请求
     * @return 会话 ID
     */
    Long createSession(SessionCreateDTO dto);

    /**
     * 当前用户的会话分页
     *
     * @param pageNum  页码
     * @param pageSize 页大小
     * @return 分页结果
     */
    PageResult<SessionVO> pageMySessions(long pageNum, long pageSize);

    /**
     * 会话消息列表（升序）
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<MessageVO> listMessages(Long sessionId);

    /**
     * 删除会话（逻辑删除，同时清理 Redis 记忆与消息）
     *
     * @param sessionId 会话 ID
     */
    void deleteSession(Long sessionId);

    /**
     * 校验会话归属并返回会话实体（越权/不存在抛业务异常）
     *
     * @param sessionId 会话 ID
     * @return 会话实体
     */
    AiSession requireOwnedSession(Long sessionId);

    /**
     * 异步落库一条消息（user/assistant）
     *
     * @param sessionId 会话 ID
     * @param role      角色
     * @param content   内容
     */
    void saveMessageAsync(Long sessionId, String role, String content);
}
