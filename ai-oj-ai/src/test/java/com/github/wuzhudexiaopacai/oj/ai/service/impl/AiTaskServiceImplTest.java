package com.github.wuzhudexiaopacai.oj.ai.service.impl;

import com.github.wuzhudexiaopacai.oj.ai.constant.AiConstants;
import com.github.wuzhudexiaopacai.oj.ai.dto.TaskSubmitDTO;
import com.github.wuzhudexiaopacai.oj.ai.vo.TaskStatusVO;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.Message;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 异步任务服务测试：任务结果按用户归属
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class AiTaskServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @InjectMocks
    private AiTaskServiceImpl aiTaskService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(LoginUser.builder().id(7L).build(), null));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitShouldBindTaskToCurrentUser() {
        TaskSubmitDTO dto = new TaskSubmitDTO();
        dto.setTaskType(AiConstants.TASK_TYPE_REVIEW);
        dto.setLanguage("java");
        dto.setCode("public class Main {}");

        String taskId = aiTaskService.submitTask(dto);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), eq(1L), eq(TimeUnit.DAYS));
        assertEquals(AiConstants.AI_TASK_KEY + taskId, keyCaptor.getValue());
        TaskStatusVO status = (TaskStatusVO) valueCaptor.getValue();
        assertEquals(7L, status.getUserId());
        assertEquals(AiConstants.TASK_PENDING, status.getStatus());
        verify(rocketMQTemplate).syncSend(eq(MqConstants.AI_TASK_TOPIC), any(Message.class));
    }

    @Test
    void getTaskShouldRejectOtherUsersTask() {
        when(valueOperations.get(AiConstants.AI_TASK_KEY + "task-1"))
                .thenReturn(new TaskStatusVO("task-1", 8L, AiConstants.TASK_SUCCESS, "其他人代码的评审结果"));

        assertThrows(BizException.class, () -> aiTaskService.getTask("task-1"));
    }

    @Test
    void getTaskShouldReturnOwnTask() {
        when(valueOperations.get(AiConstants.AI_TASK_KEY + "task-2"))
                .thenReturn(new TaskStatusVO("task-2", 7L, AiConstants.TASK_SUCCESS, "ok"));

        assertEquals("ok", aiTaskService.getTask("task-2").getResult());
    }
}
