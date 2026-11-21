package com.github.wuzhudexiaopacai.oj.judge.mq;

import com.github.wuzhudexiaopacai.oj.judge.entity.ContestRecord;
import com.github.wuzhudexiaopacai.oj.judge.mapper.ContestRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 比赛结算名次并列规则测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ContestEventConsumerTest {

    @Mock
    private ContestRecordMapper contestRecordMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ContestEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ContestEventConsumer(contestRecordMapper, redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void acmSettlementShouldShareRankForEqualAcceptAndPenalty() {
        List<ContestRecord> records = List.of(
                recordOf(1L, 2, 100L, 200),
                recordOf(2L, 2, 100L, 200),
                recordOf(3L, 1, 50L, 100));
        when(contestRecordMapper.selectList(any())).thenReturn(records);

        consumer.onMessage(messageOf(0));

        assertEquals(List.of(1, 1, 3), settledRanks());
    }

    @Test
    void oiSettlementShouldShareRankForEqualTotalScore() {
        // OI 只看总分：通过题数不同但总分相同也应并列
        List<ContestRecord> records = List.of(
                recordOf(1L, 1, 0L, 100),
                recordOf(2L, 2, 0L, 100),
                recordOf(3L, 3, 0L, 90));
        when(contestRecordMapper.selectList(any())).thenReturn(records);

        consumer.onMessage(messageOf(1));

        assertEquals(List.of(1, 1, 3), settledRanks());
    }

    private List<Integer> settledRanks() {
        ArgumentCaptor<ContestRecord> captor = ArgumentCaptor.forClass(ContestRecord.class);
        verify(contestRecordMapper, times(3)).updateById(captor.capture());
        return captor.getAllValues().stream().map(ContestRecord::getRankSnapshot).toList();
    }

    private ContestRecord recordOf(Long id, Integer acceptCount, Long penaltySeconds, Integer totalScore) {
        ContestRecord record = new ContestRecord();
        record.setId(id);
        record.setContestId(1L);
        record.setUserId(id);
        record.setUsername("user" + id);
        record.setAcceptCount(acceptCount);
        record.setPenaltySeconds(penaltySeconds);
        record.setTotalScore(totalScore);
        return record;
    }

    private ContestEventConsumer.ContestEventMessage messageOf(int contestType) {
        ContestEventConsumer.ContestEventMessage message = new ContestEventConsumer.ContestEventMessage();
        message.setEventType("end");
        message.setContestId(1L);
        message.setContestType(contestType);
        return message;
    }
}
