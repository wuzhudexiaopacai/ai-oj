package com.github.wuzhudexiaopacai.oj.judge.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.github.wuzhudexiaopacai.oj.api.dto.ContestRecordDTO;
import com.github.wuzhudexiaopacai.oj.judge.entity.ContestRecord;
import com.github.wuzhudexiaopacai.oj.judge.mapper.ContestRecordMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ContestRecordService 成绩列表映射单元测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ContestRecordServiceTest {

    @Mock
    private ContestRecordMapper contestRecordMapper;

    @Mock
    private JudgeSubmissionMapper judgeSubmissionMapper;

    @InjectMocks
    private ContestRecordService contestRecordService;

    @Test
    void listByContestCarriesRankSnapshot() {
        when(judgeSubmissionMapper.selectUserIdsByContest(1L)).thenReturn(List.of(7L));
        ContestRecord record = new ContestRecord();
        record.setContestId(1L);
        record.setUserId(7L);
        record.setUsername("alice");
        record.setTotalScore(100);
        record.setAcceptCount(2);
        record.setPenaltySeconds(30L);
        record.setRankSnapshot(1);
        when(contestRecordMapper.selectList(any())).thenReturn(List.of(record));

        List<ContestRecordDTO> result = contestRecordService.listByContest(1L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getRankSnapshot());
        assertEquals(100, result.get(0).getTotalScore());
    }

    @Test
    void listByContestSkipsDbWhenNoParticipants() {
        when(judgeSubmissionMapper.selectUserIdsByContest(2L)).thenReturn(List.of());

        assertTrue(contestRecordService.listByContest(2L).isEmpty());
        verify(contestRecordMapper, never()).selectList(any(Wrapper.class));
    }
}
