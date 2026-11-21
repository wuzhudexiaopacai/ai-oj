package com.github.wuzhudexiaopacai.oj.judge.mq;

import com.github.wuzhudexiaopacai.oj.api.dto.ContestRuntimeDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.JudgeResultMessage;
import com.github.wuzhudexiaopacai.oj.api.feign.ContestFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.judge.entity.ContestRecord;
import com.github.wuzhudexiaopacai.oj.judge.mapper.ContestRecordMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 比赛成绩消费者测试：重算式计分、罚时口径与缺失记录兜底
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ContestRecordConsumerTest {

    @Mock
    private ContestRecordMapper contestRecordMapper;

    @Mock
    private JudgeSubmissionMapper judgeSubmissionMapper;

    @Mock
    private ContestFeignClient contestFeignClient;

    @InjectMocks
    private ContestRecordConsumer consumer;

    @Test
    void nonContestMessageShouldBeIgnored() {
        consumer.onMessage(JudgeResultMessage.builder().contestId(0L).build());

        verifyNoInteractions(contestRecordMapper, judgeSubmissionMapper, contestFeignClient);
    }

    @Test
    void acceptedMessageShouldRecomputeScoreAndPenalty() {
        when(contestRecordMapper.selectOne(any())).thenReturn(recordOf(99L, 50L));
        when(judgeSubmissionMapper.selectBestScoreSumByContest(1L, 7L)).thenReturn(100);
        when(judgeSubmissionMapper.selectSolvedCountByContest(1L, 7L)).thenReturn(1);
        when(contestFeignClient.runtime(1L, null)).thenReturn(Result.ok(runtimeOf(1_000_000L)));
        LocalDateTime firstAc = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(1_300_000L), ZoneId.systemDefault());
        when(judgeSubmissionMapper.selectFirstAcceptedTimesByContest(1L, 7L))
                .thenReturn(List.of(firstAc));
        when(judgeSubmissionMapper.selectWrongAttemptsBeforeFirstAccepted(1L, 7L)).thenReturn(0);

        consumer.onMessage(messageOf(JudgeStatusEnum.ACCEPTED));

        // 罚时 = 首次 AC(1_300_000ms) - 开赛(1_000_000ms) = 300s
        verify(contestRecordMapper).mergeScore(99L, 100, 1, 300L);
    }

    @Test
    void acceptedMessageShouldAddPenaltyForWrongAttemptsBeforeFirstAc() {
        when(contestRecordMapper.selectOne(any())).thenReturn(recordOf(99L, 50L));
        when(judgeSubmissionMapper.selectBestScoreSumByContest(1L, 7L)).thenReturn(100);
        when(judgeSubmissionMapper.selectSolvedCountByContest(1L, 7L)).thenReturn(1);
        when(contestFeignClient.runtime(1L, null)).thenReturn(Result.ok(runtimeOf(1_000_000L)));
        LocalDateTime firstAc = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(1_300_000L), ZoneId.systemDefault());
        when(judgeSubmissionMapper.selectFirstAcceptedTimesByContest(1L, 7L))
                .thenReturn(List.of(firstAc));
        when(judgeSubmissionMapper.selectWrongAttemptsBeforeFirstAccepted(1L, 7L)).thenReturn(2);

        consumer.onMessage(messageOf(JudgeStatusEnum.ACCEPTED));

        // 罚时 = 首次 AC 时间差 300s + 2 次错误提交 × 1200s = 2700s
        verify(contestRecordMapper).mergeScore(99L, 100, 1, 2700L);
    }

    @Test
    void lateWrongAnswerAfterAcceptedShouldRecalculatePenalty() {
        when(contestRecordMapper.selectOne(any())).thenReturn(recordOf(99L, 50L));
        when(judgeSubmissionMapper.selectBestScoreSumByContest(1L, 7L)).thenReturn(100);
        when(judgeSubmissionMapper.selectSolvedCountByContest(1L, 7L)).thenReturn(1);
        when(contestFeignClient.runtime(1L, null)).thenReturn(Result.ok(runtimeOf(1_000_000L)));
        LocalDateTime firstAc = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(1_300_000L), ZoneId.systemDefault());
        when(judgeSubmissionMapper.selectFirstAcceptedTimesByContest(1L, 7L))
                .thenReturn(List.of(firstAc));
        when(judgeSubmissionMapper.selectWrongAttemptsBeforeFirstAccepted(1L, 7L)).thenReturn(1);

        consumer.onMessage(messageOf(JudgeStatusEnum.WRONG_ANSWER));

        // 较早提交晚于 AC 返回 WA 时，也要补上这次错误提交的 1200s 罚时
        verify(contestRecordMapper).mergeScore(99L, 100, 1, 1500L);
    }

    @Test
    void wrongAnswerWithoutAcceptedProblemShouldKeepExistingPenalty() {
        when(contestRecordMapper.selectOne(any())).thenReturn(recordOf(99L, 50L));
        when(judgeSubmissionMapper.selectBestScoreSumByContest(1L, 7L)).thenReturn(30);
        when(judgeSubmissionMapper.selectSolvedCountByContest(1L, 7L)).thenReturn(0);

        consumer.onMessage(messageOf(JudgeStatusEnum.WRONG_ANSWER));

        verify(contestRecordMapper).mergeScore(99L, 30, 0, 50L);
        verifyNoInteractions(contestFeignClient);
    }

    @Test
    void missingRecordShouldBeCreatedBeforeMerge() {
        when(contestRecordMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            ContestRecord created = invocation.getArgument(0);
            created.setId(88L);
            return 1;
        }).when(contestRecordMapper).insert(any(ContestRecord.class));
        when(judgeSubmissionMapper.selectBestScoreSumByContest(1L, 7L)).thenReturn(0);
        when(judgeSubmissionMapper.selectSolvedCountByContest(1L, 7L)).thenReturn(0);

        consumer.onMessage(messageOf(JudgeStatusEnum.WRONG_ANSWER));

        verify(contestRecordMapper).mergeScore(88L, 0, 0, 0L);
    }

    @Test
    void contestServiceUnavailableShouldKeepPenalty() {
        when(contestRecordMapper.selectOne(any())).thenReturn(recordOf(99L, 50L));
        when(judgeSubmissionMapper.selectBestScoreSumByContest(1L, 7L)).thenReturn(80);
        when(judgeSubmissionMapper.selectSolvedCountByContest(1L, 7L)).thenReturn(1);
        when(contestFeignClient.runtime(1L, null)).thenThrow(new RuntimeException("boom"));

        consumer.onMessage(messageOf(JudgeStatusEnum.ACCEPTED));

        verify(contestRecordMapper).mergeScore(99L, 80, 1, 50L);
    }

    private JudgeResultMessage messageOf(JudgeStatusEnum status) {
        return JudgeResultMessage.builder()
                .submitId("submit-1")
                .userId(7L)
                .problemId(101L)
                .contestId(1L)
                .statusCode(status.getCode())
                .score(100)
                .build();
    }

    private ContestRecord recordOf(Long id, Long penaltySeconds) {
        ContestRecord record = new ContestRecord();
        record.setId(id);
        record.setContestId(1L);
        record.setUserId(7L);
        record.setUsername("tester");
        record.setTotalScore(0);
        record.setAcceptCount(0);
        record.setPenaltySeconds(penaltySeconds);
        return record;
    }

    private ContestRuntimeDTO runtimeOf(Long startTimeMillis) {
        ContestRuntimeDTO dto = new ContestRuntimeDTO();
        dto.setStartTimeMillis(startTimeMillis);
        return dto;
    }
}
