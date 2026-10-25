package com.github.wuzhudexiaopacai.oj.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.wuzhudexiaopacai.oj.ai.constant.AiConstants;
import com.github.wuzhudexiaopacai.oj.ai.dto.SessionCreateDTO;
import com.github.wuzhudexiaopacai.oj.ai.entity.AiMessage;
import com.github.wuzhudexiaopacai.oj.ai.entity.AiSession;
import com.github.wuzhudexiaopacai.oj.ai.mapper.AiMessageMapper;
import com.github.wuzhudexiaopacai.oj.ai.mapper.AiSessionMapper;
import com.github.wuzhudexiaopacai.oj.ai.service.AiSessionService;
import com.github.wuzhudexiaopacai.oj.ai.strategy.AiSceneStrategyFactory;
import com.github.wuzhudexiaopacai.oj.ai.vo.MessageVO;
import com.github.wuzhudexiaopacai.oj.ai.vo.SessionVO;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * AI 会话服务实现
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSessionServiceImpl implements AiSessionService {

    private final AiSessionMapper sessionMapper;

    private final AiMessageMapper messageMapper;

    private final AiSceneStrategyFactory strategyFactory;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Long createSession(SessionCreateDTO dto) {
        LoginUser loginUser = SecurityUtils.requireLogin();
        Asserts.isTrue(strategyFactory.supports(dto.getScene()), "不支持的 AI 场景: " + dto.getScene());

        AiSession session = new AiSession();
        session.setUserId(loginUser.getId());
        session.setProblemId(dto.getProblemId());
        session.setScene(dto.getScene());
        session.setTitle(StringUtils.hasText(dto.getTitle()) ? dto.getTitle() : defaultTitle(dto.getScene()));
        sessionMapper.insert(session);
        return session.getId();
    }

    @Override
    public PageResult<SessionVO> pageMySessions(long pageNum, long pageSize) {
        Long userId = SecurityUtils.requireLogin().getId();
        Page<AiSession> page = sessionMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<AiSession>()
                        .eq(AiSession::getUserId, userId)
                        .orderByDesc(AiSession::getUpdateTime));
        return PageResult.of(page, this::toVO);
    }

    @Override
    public List<MessageVO> listMessages(Long sessionId) {
        requireOwnedSession(sessionId);
        List<AiMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getSessionId, sessionId)
                .orderByAsc(AiMessage::getId));
        return messages.stream().map(this::toVO).toList();
    }

    @Override
    public void deleteSession(Long sessionId) {
        requireOwnedSession(sessionId);
        sessionMapper.deleteById(sessionId);
        messageMapper.delete(new LambdaQueryWrapper<AiMessage>().eq(AiMessage::getSessionId, sessionId));
        // 清理 Redis 会话记忆
        redisTemplate.delete(RedisKeys.AI_SESSION + sessionId);
    }

    @Override
    public AiSession requireOwnedSession(Long sessionId) {
        Asserts.notNull(sessionId, "会话 ID 不能为空");
        AiSession session = sessionMapper.selectById(sessionId);
        Asserts.notNull(session, "会话不存在");
        Long userId = SecurityUtils.requireLogin().getId();
        Asserts.isTrue(Objects.equals(session.getUserId(), userId), "无权访问该会话");
        return session;
    }

    /**
     * 异步落库消息（虚拟线程执行，异常仅记录日志不影响主链路）
     */
    @Async
    @Override
    public void saveMessageAsync(Long sessionId, String role, String content) {
        try {
            AiMessage message = new AiMessage();
            message.setSessionId(sessionId);
            message.setRole(role);
            message.setContent(content);
            message.setTokens(0);
            messageMapper.insert(message);
        } catch (Exception e) {
            log.error("AI 消息异步落库失败: sessionId={}, role={}", sessionId, role, e);
        }
    }

    private SessionVO toVO(AiSession session) {
        SessionVO vo = new SessionVO();
        BeanUtils.copyProperties(session, vo);
        return vo;
    }

    private MessageVO toVO(AiMessage message) {
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(message, vo);
        return vo;
    }

    private String defaultTitle(String scene) {
        return switch (scene) {
            case AiConstants.SCENE_HINT -> "解题提示会话";
            case AiConstants.SCENE_REVIEW -> "代码评审会话";
            case AiConstants.SCENE_GENERATE -> "AI 出题会话";
            default -> "新会话";
        };
    }
}
