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
import com.github.wuzhudexiaopacai.oj.contest.entity.Contest;
import com.github.wuzhudexiaopacai.oj.contest.mapper.ContestMapper;
import com.github.wuzhudexiaopacai.oj.contest.ranking.RankingStrategy;
import com.github.wuzhudexiaopacai.oj.contest.ranking.RankingStrategyFactory;
import com.github.wuzhudexiaopacai.oj.contest.service.ContestRankService;
import com.github.wuzhudexiaopacai.oj.contest.vo.ContestRankVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 比赛排行榜服务实现（成绩实时重建 + 封榜快照 + 策略模式排序）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContestRankServiceImpl implements ContestRankService {

    /**
     * 比赛管理权限标识
     */
    private static final String AUTHORITY_CONTEST_MANAGE = "contest:manage";

    private final ContestMapper contestMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private final JudgeFeignClient judgeFeignClient;

    private final AuthFeignClient authFeignClient;

    private final RankingStrategyFactory rankingStrategyFactory;

    @Override
    public List<ContestRankVO> rank(Long contestId) {
        Contest contest = contestMapper.selectById(contestId);
        Asserts.notNull(contest, "比赛不存在");
        boolean oi = Integer.valueOf(1).equals(contest.getType());
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
                    return buildRankFromRecords(records, oi);
                }
            }
            // Redis 快照丢失（如重启/过期）时回读 DB 结算名次，避免封榜后向普通用户泄露实时榜单
            List<ContestRecordDTO> settled = loadSettledRecords(contestId);
            if (!settled.isEmpty()) {
                log.info("封榜快照缺失，已回读 rank_snapshot 兜底 contestId={} count={}", contestId, settled.size());
                return buildRankFromRecords(settled, oi);
            }
            log.warn("比赛已封榜但排行榜快照缺失，降级为实时查询 contestId={}", contestId);
        }
        // 实时榜每次从成绩源重建：增量 ZSet 会漏掉未增量写入/缓存过期的用户，导致榜单人数不全
        return rebuildRank(contest, oi);
    }

    /**
     * 回读比赛结算名次（contest_record.rank_snapshot），按名次升序返回，未结算的记录不参与
     *
     * @param contestId 比赛 ID
     * @return 已结算成绩记录；判题服务不可用或无结算数据时返回空列表
     */
    private List<ContestRecordDTO> loadSettledRecords(Long contestId) {
        try {
            Result<List<ContestRecordDTO>> result = judgeFeignClient.listContestRecord(contestId);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                return Collections.emptyList();
            }
            return result.getData().stream()
                    .filter(record -> record.getRankSnapshot() != null && record.getRankSnapshot() > 0)
                    .sorted(Comparator.comparingInt(ContestRecordDTO::getRankSnapshot))
                    .toList();
        } catch (Exception e) {
            log.warn("回读比赛结算名次失败 contestId={}", contestId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 从判题服务拉取参赛成绩 → 按赛制策略排序 → 构建排行榜
     * <p>
     * judge 侧只返回比赛期间有提交记录的用户，未参赛的报名者不会出现在榜单中。
     */
    private List<ContestRankVO> rebuildRank(Contest contest, boolean oi) {
        Result<List<ContestRecordDTO>> result = judgeFeignClient.listContestRecord(contest.getId());
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BizException("比赛成绩服务暂不可用，请稍后重试");
        }
        List<ContestRecordDTO> records = result.getData();
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        RankingStrategy strategy = rankingStrategyFactory.getStrategy(contest.getType());
        return buildRankFromRecords(strategy.sort(records), oi);
    }

    /**
     * 从有序成绩记录构建排行榜 VO（实时重建与封榜快照共用）
     * <p>
     * 名次按赛制并列：OI 同总分并列，ACM 同通过题数且同罚时并列；
     * 并列后下一名跳号（1、1、3），与主流 OJ 排行榜一致。
     *
     * @param sorted 已按赛制排序的成绩记录
     * @param oi     是否 OI 赛制
     * @return 排行榜
     */
    private List<ContestRankVO> buildRankFromRecords(List<ContestRecordDTO> sorted, boolean oi) {
        List<ContestRankVO> ranks = new ArrayList<>(sorted.size());
        int rankNo = 0;
        int position = 0;
        ContestRecordDTO previous = null;
        for (ContestRecordDTO record : sorted) {
            position++;
            if (previous == null || !sameRank(previous, record, oi)) {
                rankNo = position;
            }
            ContestRankVO vo = new ContestRankVO();
            vo.setRank(rankNo);
            vo.setUserId(record.getUserId());
            vo.setUsername(record.getUsername());
            vo.setAcceptCount(nullToZero(record.getAcceptCount()));
            vo.setPenaltySeconds(nullToZero(record.getPenaltySeconds()));
            vo.setTotalScore(nullToZero(record.getTotalScore()));
            ranks.add(vo);
            previous = record;
        }
        fillUserBrief(ranks);
        return ranks;
    }

    /**
     * 两条成绩是否应并列同一名次：OI 比较总分，ACM 比较通过题数与罚时
     */
    private boolean sameRank(ContestRecordDTO previous, ContestRecordDTO current, boolean oi) {
        if (oi) {
            return nullToZero(previous.getTotalScore()) == nullToZero(current.getTotalScore());
        }
        return nullToZero(previous.getAcceptCount()) == nullToZero(current.getAcceptCount())
                && nullToZero(previous.getPenaltySeconds()) == nullToZero(current.getPenaltySeconds());
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
                rank.setIntro(brief.getIntro());
                if (!StringUtils.hasText(rank.getUsername())) {
                    rank.setUsername(brief.getUsername());
                }
            }
        }
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }
}
