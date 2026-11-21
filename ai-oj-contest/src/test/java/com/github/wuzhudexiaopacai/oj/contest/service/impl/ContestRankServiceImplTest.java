package com.github.wuzhudexiaopacai.oj.contest.service.impl;

import com.github.wuzhudexiaopacai.oj.api.dto.ContestRecordDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.UserBriefDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.AuthFeignClient;
import com.github.wuzhudexiaopacai.oj.api.feign.JudgeFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.contest.entity.Contest;
import com.github.wuzhudexiaopacai.oj.contest.mapper.ContestMapper;
import com.github.wuzhudexiaopacai.oj.contest.ranking.RankingStrategy;
import com.github.wuzhudexiaopacai.oj.contest.ranking.RankingStrategyFactory;
import com.github.wuzhudexiaopacai.oj.contest.vo.ContestRankVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ContestRankServiceImpl 封榜快照与 rank_snapshot 回读单元测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ContestRankServiceImplTest {

    @Mock
    private ContestMapper contestMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private JudgeFeignClient judgeFeignClient;

    @Mock
    private AuthFeignClient authFeignClient;

    @Mock
    private RankingStrategyFactory rankingStrategyFactory;

    @InjectMocks
    private ContestRankServiceImpl rankService;

    @BeforeEach
    void stubCommon() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // 昵称头像批量查询统一降级为空，不影响名次断言
        lenient().doReturn(Result.ok(Collections.<UserBriefDTO>emptyList()))
                .when(authFeignClient).batchGetUserBrief(any());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 构造比赛，sealed=true 表示已过封榜时间
     */
    private Contest contestOf(int type, boolean sealed) {
        Contest contest = new Contest();
        contest.setId(1L);
        contest.setType(type);
        contest.setSealTime(sealed ? LocalDateTime.now().minusMinutes(1) : null);
        return contest;
    }

    private ContestRecordDTO recordOf(Long userId, Integer totalScore, Integer rankSnapshot) {
        return recordOf(userId, totalScore, 1, 0L, rankSnapshot);
    }

    private ContestRecordDTO recordOf(Long userId, Integer totalScore, Integer acceptCount,
                                      Long penaltySeconds, Integer rankSnapshot) {
        ContestRecordDTO record = new ContestRecordDTO();
        record.setContestId(1L);
        record.setUserId(userId);
        record.setUsername("user" + userId);
        record.setTotalScore(totalScore);
        record.setAcceptCount(acceptCount);
        record.setPenaltySeconds(penaltySeconds);
        record.setRankSnapshot(rankSnapshot);
        return record;
    }

    @Test
    void sealedRankPrefersRedisSnapshot() {
        when(contestMapper.selectById(1L)).thenReturn(contestOf(0, true));
        List<ContestRecordDTO> snapshot = List.of(
                recordOf(2L, 200, 2, 100L, null), recordOf(1L, 100, 1, 50L, null));
        when(valueOperations.get(anyString())).thenReturn(snapshot);

        List<ContestRankVO> ranks = rankService.rank(1L);

        assertEquals(2, ranks.size());
        assertEquals(1, ranks.get(0).getRank());
        assertEquals(2L, ranks.get(0).getUserId());
        verify(judgeFeignClient, never()).listContestRecord(anyLong());
    }

    @Test
    void acmRankSharesRankWhenAcceptAndPenaltyEqual() {
        when(contestMapper.selectById(1L)).thenReturn(contestOf(0, false));
        ContestRecordDTO first = recordOf(1L, 200, 2, 100L, null);
        ContestRecordDTO second = recordOf(2L, 200, 2, 100L, null);
        ContestRecordDTO third = recordOf(3L, 100, 1, 50L, null);
        doReturn(Result.ok(List.of(first, second, third))).when(judgeFeignClient).listContestRecord(1L);
        RankingStrategy strategy = mock(RankingStrategy.class);
        when(rankingStrategyFactory.getStrategy(0)).thenReturn(strategy);
        when(strategy.sort(anyList())).thenReturn(List.of(first, second, third));

        List<ContestRankVO> ranks = rankService.rank(1L);

        assertEquals(List.of(1, 1, 3), ranks.stream().map(ContestRankVO::getRank).toList());
    }

    @Test
    void oiRankSharesRankWhenTotalScoreEqual() {
        when(contestMapper.selectById(1L)).thenReturn(contestOf(1, false));
        ContestRecordDTO first = recordOf(1L, 100, 2, 0L, null);
        ContestRecordDTO second = recordOf(2L, 100, 2, 0L, null);
        ContestRecordDTO third = recordOf(3L, 90, 1, 0L, null);
        doReturn(Result.ok(List.of(first, second, third))).when(judgeFeignClient).listContestRecord(1L);
        RankingStrategy strategy = mock(RankingStrategy.class);
        when(rankingStrategyFactory.getStrategy(1)).thenReturn(strategy);
        when(strategy.sort(anyList())).thenReturn(List.of(first, second, third));

        List<ContestRankVO> ranks = rankService.rank(1L);

        assertEquals(List.of(1, 1, 3), ranks.stream().map(ContestRankVO::getRank).toList());
    }

    @Test
    void sealedRankFallsBackToRankSnapshotWhenRedisMissing() {
        when(contestMapper.selectById(1L)).thenReturn(contestOf(1, true));
        when(valueOperations.get(anyString())).thenReturn(null);
        // 判题服务返回未排序的已结算记录：rankSnapshot 1 在后
        doReturn(Result.ok(List.of(recordOf(1L, 90, 2), recordOf(2L, 110, 1)))).when(judgeFeignClient)
                .listContestRecord(1L);

        List<ContestRankVO> ranks = rankService.rank(1L);

        assertEquals(2, ranks.size());
        assertEquals(1, ranks.get(0).getRank());
        assertEquals(2L, ranks.get(0).getUserId());
        assertEquals(2, ranks.get(1).getRank());
        // 走快照兜底，不应再实时排序
        verify(rankingStrategyFactory, never()).getStrategy(anyInt());
    }

    @Test
    void sealedRankIgnoresUnsettledRecords() {
        when(contestMapper.selectById(1L)).thenReturn(contestOf(0, true));
        when(valueOperations.get(anyString())).thenReturn(null);
        // 全部未结算（rankSnapshot 为 null）→ 回读无数据，最终降级为实时榜
        List<ContestRecordDTO> records = List.of(recordOf(1L, 90, null), recordOf(2L, 110, null));
        doReturn(Result.ok(records)).when(judgeFeignClient).listContestRecord(1L);
        RankingStrategy strategy = mock(RankingStrategy.class);
        when(rankingStrategyFactory.getStrategy(0)).thenReturn(strategy);
        when(strategy.sort(anyList())).thenReturn(List.of(records.get(1), records.get(0)));

        List<ContestRankVO> ranks = rankService.rank(1L);

        assertEquals(2L, ranks.get(0).getUserId());
        verify(strategy).sort(anyList());
    }

    @Test
    void unsealedRankRebuildsLive() {
        when(contestMapper.selectById(1L)).thenReturn(contestOf(0, false));
        List<ContestRecordDTO> records = List.of(recordOf(1L, 90, null), recordOf(2L, 110, null));
        doReturn(Result.ok(records)).when(judgeFeignClient).listContestRecord(1L);
        RankingStrategy strategy = mock(RankingStrategy.class);
        when(rankingStrategyFactory.getStrategy(0)).thenReturn(strategy);
        when(strategy.sort(anyList())).thenReturn(List.of(records.get(1), records.get(0)));

        List<ContestRankVO> ranks = rankService.rank(1L);

        assertEquals(2, ranks.size());
        assertEquals(2L, ranks.get(0).getUserId());
        verify(judgeFeignClient).listContestRecord(1L);
    }

    @Test
    void sealedAdminStillSeesLiveRank() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        LoginUser.builder().id(99L).authorities(List.of("contest:manage")).build(), null));
        when(contestMapper.selectById(1L)).thenReturn(contestOf(0, true));
        List<ContestRecordDTO> records = List.of(recordOf(1L, 90, null), recordOf(2L, 110, null));
        doReturn(Result.ok(records)).when(judgeFeignClient).listContestRecord(1L);
        RankingStrategy strategy = mock(RankingStrategy.class);
        when(rankingStrategyFactory.getStrategy(0)).thenReturn(strategy);
        when(strategy.sort(anyList())).thenReturn(List.of(records.get(1), records.get(0)));

        List<ContestRankVO> ranks = rankService.rank(1L);

        assertEquals(2, ranks.size());
        // 管理员不走封榜快照
        verify(valueOperations, never()).get(anyString());
    }
}
