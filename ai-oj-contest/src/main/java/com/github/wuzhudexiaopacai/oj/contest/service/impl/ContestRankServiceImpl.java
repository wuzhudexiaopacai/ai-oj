package com.github.wuzhudexiaopacai.oj.contest.service.impl;

import com.github.wuzhudexiaopacai.oj.api.dto.ContestRecordDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.UserBriefDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.AuthFeignClient;
import com.github.wuzhudexiaopacai.oj.api.feign.JudgeFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils;
import com.github.wuzhudexiaopacai.oj.contest.config.ContestProperties;
import com.github.wuzhudexiaopacai.oj.contest.entity.Contest;
import com.github.wuzhudexiaopacai.oj.contest.mapper.ContestMapper;
import com.github.wuzhudexiaopacai.oj.contest.ranking.RankingStrategy;
import com.github.wuzhudexiaopacai.oj.contest.ranking.RankingStrategyFactory;
import com.github.wuzhudexiaopacai.oj.contest.service.ContestRankService;
import com.github.wuzhudexiaopacai.oj.contest.vo.ContestRankVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 比赛排行榜服务实现（ZSet 实时榜 + 封榜快照 + 策略模式排序）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContestRankServiceImpl implements ContestRankService {

    /**
     * ACM 赛制 ZSet score 编码基数：score = acceptCount * 1e9 - penaltySeconds，
     * 罚时小于 1e9 秒（约 31 年）时编码无损、可用 Math.ceil 反解
     */
    private static final double ACM_SCORE_BASE = 1_000_000_000.0D;

    /**
     * 排行榜重建分布式锁前缀：+ contestId
     */
    private static final String RANK_LOCK_KEY = "contest:lock:rank:";

    /**
     * 比赛管理权限标识
     */
    private static final String AUTHORITY_CONTEST_MANAGE = "contest:manage";

    private final ContestMapper contestMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private final RedissonClient redissonClient;

    private final JudgeFeignClient judgeFeignClient;

    private final AuthFeignClient authFeignClient;

    private final RankingStrategyFactory rankingStrategyFactory;

    private final ContestProperties contestProperties;

    @Override
    public List<ContestRankVO> rank(Long contestId) {
        Contest contest = contestMapper.selectById(contestId);
        Asserts.notNull(contest, "比赛不存在");
        LoginUser loginUser = SecurityUtils.currentUser();
        boolean admin = loginUser != null && loginUser.hasAuthority(AUTHORITY_CONTEST_MANAGE);
        boolean sealed = contest.getSealTime() != null && !LocalDateTime.now().isBefore(contest.getSealTime());
        if (sealed && !admin) {
            // 封榜后普通用户只看快照（judge 服务消费比赛结束事件时写入，契约：List<ContestRecordDTO>，api 共享类跨服务反序列化无类型风险）
            Object snapshot = redisTemplate.opsForValue().get(RedisKeys.CONTEST_RANK + contestId);
            if (snapshot instanceof List<?> list && !list.isEmpty()) {
                List<ContestRecordDTO> records = list.stream()
                        .filter(ContestRecordDTO.class::isInstance)
                        .map(ContestRecordDTO.class::cast)
                        .toList();
                if (!records.isEmpty()) {
                    return buildRankFromRecords(records);
                }
            }
            log.warn("比赛已封榜但排行榜快照缺失，降级为实时查询 contestId={}", contestId);
        }
        // 先 ZREVRANGE 读实时榜 ZSet（member=userId，平时由 judge 服务按判题结果增量维护）
        String zsetKey = RedisKeys.CONTEST_RANK_ZSET + contestId;
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(zsetKey, 0, -1);
        if (tuples != null && !tuples.isEmpty()) {
            return buildRankFromZSet(contest, tuples);
        }
        // 缓存 miss：分布式锁 + 双重检查重建（防缓存击穿）
        RLock lock = redissonClient.getLock(RANK_LOCK_KEY + contestId);
        lock.lock();
        try {
            tuples = redisTemplate.opsForZSet().reverseRangeWithScores(zsetKey, 0, -1);
            if (tuples != null && !tuples.isEmpty()) {
                return buildRankFromZSet(contest, tuples);
            }
            return rebuildRankCache(contest, zsetKey);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 重建排行榜 ZSet：拉取全量成绩 → 策略模式按赛制排序 → 回写 ZSet（TTL 取配置）
     */
    private List<ContestRankVO> rebuildRankCache(Contest contest, String zsetKey) {
        Result<List<ContestRecordDTO>> result = judgeFeignClient.listContestRecord(contest.getId());
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BizException("比赛成绩服务暂不可用，请稍后重试");
        }
        RankingStrategy strategy = rankingStrategyFactory.getStrategy(contest.getType());
        List<ContestRecordDTO> sorted = strategy.sort(result.getData());
        if (sorted.isEmpty()) {
            return Collections.emptyList();
        }
        boolean acm = isAcm(contest);
        // 重建 ZSet（member=userId 字符串，score 按赛制编码，与 judge 服务实时维护的编码规则一致）
        redisTemplate.delete(zsetKey);
        for (ContestRecordDTO record : sorted) {
            double score = acm
                    ? nullToZero(record.getAcceptCount()) * ACM_SCORE_BASE - nullToZero(record.getPenaltySeconds())
                    : nullToZero(record.getTotalScore());
            redisTemplate.opsForZSet().add(zsetKey, String.valueOf(record.getUserId()), score);
        }
        redisTemplate.expire(zsetKey, contestProperties.getRankTtlSeconds(), TimeUnit.SECONDS);
        return buildRankFromRecords(sorted);
    }

    /**
     * 从有序成绩记录构建排行榜 VO（实时重建与封榜快照共用）
     */
    private List<ContestRankVO> buildRankFromRecords(List<ContestRecordDTO> sorted) {
        List<ContestRankVO> ranks = new ArrayList<>(sorted.size());
        int rankNo = 0;
        for (ContestRecordDTO record : sorted) {
            ContestRankVO vo = new ContestRankVO();
            vo.setRank(++rankNo);
            vo.setUserId(record.getUserId());
            vo.setUsername(record.getUsername());
            vo.setAcceptCount(nullToZero(record.getAcceptCount()));
            vo.setPenaltySeconds(nullToZero(record.getPenaltySeconds()));
            vo.setTotalScore(nullToZero(record.getTotalScore()));
            ranks.add(vo);
        }
        fillUserBrief(ranks);
        return ranks;
    }

    /**
     * 从 ZSet 缓存构建排行榜（score 按赛制反解出业务字段，避免回源数据库）
     */
    private List<ContestRankVO> buildRankFromZSet(Contest contest, Set<ZSetOperations.TypedTuple<Object>> tuples) {
        boolean acm = isAcm(contest);
        List<ContestRankVO> ranks = new ArrayList<>(tuples.size());
        int rankNo = 0;
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Object value = tuple.getValue();
            Double score = tuple.getScore();
            if (value == null || score == null) {
                continue;
            }
            ContestRankVO vo = new ContestRankVO();
            vo.setRank(++rankNo);
            vo.setUserId(Long.valueOf(value.toString()));
            if (acm) {
                // 反解：acceptCount = ceil(score / 1e9)，penaltySeconds = acceptCount * 1e9 - score
                long acceptCount = (long) Math.ceil(score / ACM_SCORE_BASE);
                vo.setAcceptCount((int) acceptCount);
                vo.setPenaltySeconds((long) (acceptCount * ACM_SCORE_BASE - score));
                vo.setTotalScore(0);
            } else {
                vo.setTotalScore((int) Math.round(score));
                vo.setAcceptCount(0);
                vo.setPenaltySeconds(0L);
            }
            ranks.add(vo);
        }
        fillUserBrief(ranks);
        return ranks;
    }

    /**
     * 批量补充昵称/头像（一次 Feign 调用批量获取，避免循环单查放大 RPC）
     */
    private void fillUserBrief(List<ContestRankVO> ranks) {
        if (ranks.isEmpty()) {
            return;
        }
        List<Long> userIds = ranks.stream().map(ContestRankVO::getUserId).distinct().toList();
        Map<Long, UserBriefDTO> briefMap;
        try {
            Result<List<UserBriefDTO>> result = authFeignClient.batchGetUserBrief(userIds);
            briefMap = result != null && result.isSuccess() && result.getData() != null
                    ? result.getData().stream()
                    .collect(Collectors.toMap(UserBriefDTO::getId, Function.identity(), (a, b) -> a))
                    : Collections.emptyMap();
        } catch (Exception e) {
            // 用户服务不可用降级：排行榜照常返回，昵称/头像缺失
            log.warn("批量获取用户简要信息失败，排行榜降级展示 userIds={}", userIds, e);
            briefMap = Collections.emptyMap();
        }
        for (ContestRankVO rank : ranks) {
            UserBriefDTO brief = briefMap.get(rank.getUserId());
            if (brief != null) {
                rank.setNickname(brief.getNickname());
                rank.setAvatar(brief.getAvatar());
                if (!StringUtils.hasText(rank.getUsername())) {
                    rank.setUsername(brief.getUsername());
                }
            }
        }
    }

    /**
     * 是否 ACM 赛制（null/未知按 ACM 兜底，与策略工厂保持一致）
     */
    private boolean isAcm(Contest contest) {
        return contest.getType() == null || contest.getType() == RankingStrategyFactory.TYPE_ACM;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }
}
