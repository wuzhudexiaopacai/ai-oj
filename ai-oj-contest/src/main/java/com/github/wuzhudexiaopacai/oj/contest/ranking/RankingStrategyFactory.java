package com.github.wuzhudexiaopacai.oj.contest.ranking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 排行榜策略工厂：按比赛赛制（contest.type：0=ACM 1=OI）选择排序策略
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class RankingStrategyFactory {

    /**
     * 赛制：ACM
     */
    public static final int TYPE_ACM = 0;

    /**
     * 赛制：OI
     */
    public static final int TYPE_OI = 1;

    private final AcmRankingStrategy acmRankingStrategy;

    private final OiRankingStrategy oiRankingStrategy;

    /**
     * 按赛制获取排序策略（null/未知赛制兜底按 ACM 处理）
     *
     * @param contestType 赛制
     * @return 排序策略
     */
    public RankingStrategy getStrategy(Integer contestType) {
        if (contestType != null && contestType == TYPE_OI) {
            return oiRankingStrategy;
        }
        return acmRankingStrategy;
    }
}
