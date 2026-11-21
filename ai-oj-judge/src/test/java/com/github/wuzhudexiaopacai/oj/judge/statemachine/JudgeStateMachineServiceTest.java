package com.github.wuzhudexiaopacai.oj.judge.statemachine;

import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.config.StateMachineFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 判题终态落库并发控制测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class JudgeStateMachineServiceTest {

    @Mock
    private StateMachineFactory<JudgeStatusEnum, JudgeEvent> stateMachineFactory;

    @Mock
    private JudgeSubmissionMapper judgeSubmissionMapper;

    @InjectMocks
    private JudgeStateMachineService stateMachineService;

    @Test
    void terminalTransitionShouldReturnTrueOnlyWhenPersisted() {
        JudgeSubmission submission = submissionOf(JudgeStatusEnum.JUDGING);
        when(judgeSubmissionMapper.updateById(submission)).thenReturn(1);

        boolean transitioned = stateMachineService.transitionToTerminal(
                submission, JudgeStatusEnum.SYSTEM_ERROR, "timeout");

        assertTrue(transitioned);
        assertTrue(JudgeStatusEnum.fromCode(submission.getStatus()).isTerminal());
    }

    @Test
    void optimisticLockConflictShouldReturnFalse() {
        JudgeSubmission submission = submissionOf(JudgeStatusEnum.JUDGING);
        when(judgeSubmissionMapper.updateById(submission)).thenReturn(0);

        boolean transitioned = stateMachineService.transitionToTerminal(
                submission, JudgeStatusEnum.SYSTEM_ERROR, "timeout");

        assertFalse(transitioned);
    }

    @Test
    void existingTerminalShouldNotUpdateAgain() {
        JudgeSubmission submission = submissionOf(JudgeStatusEnum.ACCEPTED);

        boolean transitioned = stateMachineService.transitionToTerminal(
                submission, JudgeStatusEnum.SYSTEM_ERROR, "late failure");

        assertFalse(transitioned);
        verify(judgeSubmissionMapper, never()).updateById(submission);
    }

    private JudgeSubmission submissionOf(JudgeStatusEnum status) {
        JudgeSubmission submission = new JudgeSubmission();
        submission.setId(1L);
        submission.setSubmitId("submit-1");
        submission.setStatus(status.getCode());
        submission.setVersion(1);
        return submission;
    }
}
