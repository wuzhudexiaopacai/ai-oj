package com.github.wuzhudexiaopacai.oj.ai.service.impl;

import com.github.wuzhudexiaopacai.oj.ai.entity.AiMessage;
import com.github.wuzhudexiaopacai.oj.ai.mapper.AiMessageMapper;
import com.github.wuzhudexiaopacai.oj.ai.mapper.AiSessionMapper;
import com.github.wuzhudexiaopacai.oj.ai.strategy.AiSceneStrategyFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AiSessionServiceImpl 消息落库与 token 统计单元测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class AiSessionServiceImplTest {

    @Mock
    private AiSessionMapper sessionMapper;

    @Mock
    private AiMessageMapper messageMapper;

    @Mock
    private AiSceneStrategyFactory strategyFactory;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private AiSessionServiceImpl sessionService;

    @Test
    void saveMessagePersistsTokens() {
        sessionService.saveMessageAsync(10L, "assistant", "回答内容", 128);

        ArgumentCaptor<AiMessage> captor = ArgumentCaptor.forClass(AiMessage.class);
        verify(messageMapper).insert(captor.capture());
        AiMessage saved = captor.getValue();
        assertEquals(10L, saved.getSessionId());
        assertEquals("assistant", saved.getRole());
        assertEquals("回答内容", saved.getContent());
        assertEquals(128, saved.getTokens());
    }

    @Test
    void nullOrNegativeTokensFallBackToZero() {
        sessionService.saveMessageAsync(11L, "user", "问题", null);
        sessionService.saveMessageAsync(11L, "user", "问题", -5);

        ArgumentCaptor<AiMessage> captor = ArgumentCaptor.forClass(AiMessage.class);
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals(0, captor.getAllValues().get(0).getTokens());
        assertEquals(0, captor.getAllValues().get(1).getTokens());
    }

    @Test
    void insertFailureIsSwallowed() {
        when(messageMapper.insert(any(AiMessage.class))).thenThrow(new RuntimeException("db down"));
        assertDoesNotThrow(() -> sessionService.saveMessageAsync(12L, "assistant", "回答", 5));
    }
}
