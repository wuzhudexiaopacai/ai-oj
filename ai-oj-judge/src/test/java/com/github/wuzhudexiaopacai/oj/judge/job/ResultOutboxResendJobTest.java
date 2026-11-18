package com.github.wuzhudexiaopacai.oj.judge.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.api.dto.JudgeResultMessage;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeOutbox;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeOutboxMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 结果 Outbox 补投任务测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ResultOutboxResendJobTest {

    @Mock
    private JudgeOutboxMapper judgeOutboxMapper;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ResultOutboxResendJob resultOutboxResendJob;

    @Test
    void resendShouldMarkSentWhenSendSucceeds() throws Exception {
        JudgeOutbox outbox = outboxOf("submit-1");
        when(judgeOutboxMapper.selectList(any())).thenReturn(List.of(outbox));

        resultOutboxResendJob.executeInternal(null);

        verify(rocketMQTemplate).syncSend(eq(MqConstants.JUDGE_RESULT_TOPIC), any(Message.class), eq(5000L));
        assertEquals(JudgeOutbox.STATUS_SENT, outbox.getStatus());
        verify(judgeOutboxMapper).updateById(outbox);
    }

    @Test
    void resendShouldBackoffWhenSendFails() throws Exception {
        JudgeOutbox outbox = outboxOf("submit-1");
        when(judgeOutboxMapper.selectList(any())).thenReturn(List.of(outbox));
        when(rocketMQTemplate.syncSend(eq(MqConstants.JUDGE_RESULT_TOPIC), any(Message.class), eq(5000L)))
                .thenThrow(new MessagingException("mq down"));

        LocalDateTime before = LocalDateTime.now();
        resultOutboxResendJob.executeInternal(null);

        assertEquals(JudgeOutbox.STATUS_PENDING, outbox.getStatus());
        assertEquals(1, outbox.getRetryCount());
        assertTrue(outbox.getNextRetryTime().isAfter(before), "失败后应延后排期重试");
        verify(judgeOutboxMapper).updateById(outbox);
    }

    @Test
    void backoffShouldGrowExponentiallyAndCap() {
        assertEquals(10L, ResultOutboxResendJob.backoffSeconds(1));
        assertEquals(20L, ResultOutboxResendJob.backoffSeconds(2));
        assertEquals(40L, ResultOutboxResendJob.backoffSeconds(3));
        assertEquals(300L, ResultOutboxResendJob.backoffSeconds(10));
    }

    private JudgeOutbox outboxOf(String submitId) throws Exception {
        JudgeResultMessage message = JudgeResultMessage.builder()
                .submitId(submitId)
                .userId(7L)
                .problemId(100L)
                .statusCode(3)
                .score(100)
                .build();
        JudgeOutbox outbox = new JudgeOutbox();
        outbox.setSubmitId(submitId);
        outbox.setPayload(objectMapper.writeValueAsString(message));
        outbox.setStatus(JudgeOutbox.STATUS_PENDING);
        outbox.setRetryCount(0);
        outbox.setNextRetryTime(LocalDateTime.now().minusSeconds(1));
        return outbox;
    }
}
