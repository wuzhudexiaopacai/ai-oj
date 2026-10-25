package com.github.wuzhudexiaopacai.oj.contest.ranking;

import com.github.wuzhudexiaopacai.oj.api.dto.ContestRecordDTO;

import java.util.List;

/**
 * 排行榜排序策略（策略模式：按赛制选择不同排序规则）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface RankingStrategy {

    /**
     * 对比赛成绩记录排序（返回新列表，不修改入参）
     *
     * @param records 成绩记录（未排序）
     * @return 排序后的成绩记录
     */
    List<ContestRecordDTO> sort(List<ContestRecordDTO> records);
}
