package com.github.wuzhudexiaopacai.oj.judge.job;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import com.github.wuzhudexiaopacai.oj.judge.service.JudgeExecuteService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 卡死提交补偿并发测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class StuckSubmissionScanJobTest {

    @Mock
    private JudgeSubmissionMapper judgeSubmissionMapper;

    @Mock
    private JudgeExecuteService judgeExecuteService;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @InjectMocks
    private StuckSubmissionScanJob stuckSubmissionScanJob;

    @BeforeAll
    static void initMybatisPlus() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                JudgeSubmission.class);
    }

    @Test
    void terminalConflictShouldNotBroadcastStaleSystemError() {
        JudgeSubmission submission = stuckSubmission();
        when(judgeSubmissionMapper.selectList(any())).thenReturn(List.of(submission));
        when(judgeExecuteService.transitionToTerminalAndSaveOutbox(eq(submission),
                eq(JudgeStatusEnum.SYSTEM_ERROR), anyString())).thenReturn(false);

        stuckSubmissionScanJob.executeInternal(null);

        verify(judgeExecuteService, never()).broadcast(any());
        verifyNoInteractions(rocketMQTemplate);
    }

    @Test
    void successfulTerminalTransitionShouldBroadcast() {
        JudgeSubmission submission = stuckSubmission();
        when(judgeSubmissionMapper.selectList(any())).thenReturn(List.of(submission));
        when(judgeExecuteService.transitionToTerminalAndSaveOutbox(eq(submission),
                eq(JudgeStatusEnum.SYSTEM_ERROR), anyString())).thenReturn(true);

        stuckSubmissionScanJob.executeInternal(null);

        verify(judgeExecuteService).broadcast(submission);
        verifyNoInteractions(rocketMQTemplate);
    }

    private JudgeSubmission stuckSubmission() {
        JudgeSubmission submission = new JudgeSubmission();
        submission.setId(1L);
        submission.setSubmitId("submit-1");
        submission.setStatus(JudgeStatusEnum.JUDGING.getCode());
        submission.setRetryCount(MqConstants.JUDGE_MAX_RETRY);
        submission.setUpdateTime(LocalDateTime.now().minusMinutes(30));
        submission.setVersion(1);
        return submission;
    }
}
