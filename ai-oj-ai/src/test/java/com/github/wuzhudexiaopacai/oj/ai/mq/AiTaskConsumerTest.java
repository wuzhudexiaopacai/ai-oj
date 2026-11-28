package com.github.wuzhudexiaopacai.oj.ai.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.ai.constant.AiConstants;
import com.github.wuzhudexiaopacai.oj.ai.dto.AiTaskMessage;
import com.github.wuzhudexiaopacai.oj.ai.strategy.AiSceneStrategy;
import com.github.wuzhudexiaopacai.oj.ai.strategy.AiSceneStrategyFactory;
import com.github.wuzhudexiaopacai.oj.ai.vo.TaskStatusVO;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 异步任务消费者测试：消息解析容错、有限重试、结果幂等
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class AiTaskConsumerTest {

    private static final String TASK_KEY = AiConstants.AI_TASK_KEY + "task-1";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private AiSceneStrategyFactory strategyFactory;

    @Mock
    private AiSceneStrategy strategy;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AiTaskConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AiTaskConsumer(redisTemplate, chatClient, strategyFactory, objectMapper);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void malformedMessageShouldBeDiscardedWithoutRetry() {
        assertDoesNotThrow(() -> consumer.onMessage(messageOf("not-json", 0)));

        verify(strategyFactory, never()).getStrategy(anyString());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void retryNotExhaustedShouldRethrowForRocketMqRetry() {
        when(strategyFactory.getStrategy("generate")).thenThrow(new RuntimeException("llm timeout"));

        assertThrows(RuntimeException.class, () -> consumer.onMessage(messageOf(validBody(), 0)));

        // 失败状态先落缓存，未超限时重抛交由 RocketMQ 重试
        verify(valueOperations).set(eq(TASK_KEY), any(TaskStatusVO.class), eq(1L), eq(TimeUnit.DAYS));
    }

    @Test
    void retryExhaustedShouldMarkFailedAndAck() {
        when(strategyFactory.getStrategy("generate")).thenThrow(new RuntimeException("llm timeout"));

        assertDoesNotThrow(() -> consumer.onMessage(messageOf(validBody(), 2)));

        ArgumentCaptor<TaskStatusVO> captor = ArgumentCaptor.forClass(TaskStatusVO.class);
        verify(valueOperations).set(eq(TASK_KEY), captor.capture(), eq(1L), eq(TimeUnit.DAYS));
        assertEquals(AiConstants.TASK_FAILED, captor.getValue().getStatus());
    }

    @Test
    void successResultShouldSkipDuplicateConsumption() {
        TaskStatusVO done = new TaskStatusVO("task-1", 1L, AiConstants.TASK_SUCCESS, "ok");
        when(valueOperations.get(TASK_KEY)).thenReturn(done);

        consumer.onMessage(messageOf(validBody(), 0));

        verify(strategyFactory, never()).getStrategy(anyString());
    }

    private MessageExt messageOf(String body, int reconsumeTimes) {
        MessageExt ext = new MessageExt();
        ext.setBody(body.getBytes(StandardCharsets.UTF_8));
        ext.setReconsumeTimes(reconsumeTimes);
        ext.setMsgId("msg-1");
        return ext;
    }

    private String validBody() {
        return assertDoesNotThrow(() -> objectMapper.writeValueAsString(AiTaskMessage.builder()
                .taskId("task-1")
                .userId(1L)
                .taskType("generate")
                .params(Map.of("topic", "dp"))
                .build()));
    }
}
