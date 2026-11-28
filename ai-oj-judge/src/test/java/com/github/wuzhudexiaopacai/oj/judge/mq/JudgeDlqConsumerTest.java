package com.github.wuzhudexiaopacai.oj.judge.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import com.github.wuzhudexiaopacai.oj.judge.entity.BizDeadLetter;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import com.github.wuzhudexiaopacai.oj.judge.mapper.BizDeadLetterMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import com.github.wuzhudexiaopacai.oj.judge.service.JudgeExecuteService;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 判题死信兜底并发测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class JudgeDlqConsumerTest {

    @Mock
    private BizDeadLetterMapper bizDeadLetterMapper;

    @Mock
    private JudgeSubmissionMapper judgeSubmissionMapper;

    @Mock
    private JudgeExecuteService judgeExecuteService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private JudgeDlqConsumer judgeDlqConsumer;

    @Test
    void terminalConflictShouldArchiveWithoutBroadcastingStaleResult() throws Exception {
        JudgeSubmission submission = new JudgeSubmission();
        submission.setId(1L);
        submission.setSubmitId("submit-1");
        submission.setStatus(JudgeStatusEnum.JUDGING.getCode());
        when(judgeSubmissionMapper.selectById(1L)).thenReturn(submission);
        when(judgeExecuteService.transitionToTerminalAndSaveOutbox(eq(submission),
                eq(JudgeStatusEnum.SYSTEM_ERROR), anyString())).thenReturn(false);
        JudgeSubmitMessage submitMessage = JudgeSubmitMessage.builder()
                .submitId("submit-1").submissionId(1L).build();
        MessageExt messageExt = new MessageExt();
        messageExt.setMsgId("msg-1");
        messageExt.setBody(objectMapper.writeValueAsString(submitMessage).getBytes(StandardCharsets.UTF_8));

        judgeDlqConsumer.onMessage(messageExt);

        verify(judgeExecuteService, never()).broadcast(any());
        verify(bizDeadLetterMapper).insert(any(BizDeadLetter.class));
    }
}
