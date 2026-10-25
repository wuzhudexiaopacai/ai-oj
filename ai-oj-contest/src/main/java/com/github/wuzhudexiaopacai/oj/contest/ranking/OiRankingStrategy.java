package com.github.wuzhudexiaopacai.oj.contest.ranking;

import com.github.wuzhudexiaopacai.oj.api.dto.ContestRecordDTO;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OI 赛制排序策略：总分降序
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Component
public class OiRankingStrategy implements RankingStrategy {

    @Override
    public List<ContestRecordDTO> sort(List<ContestRecordDTO> records) {
        return records.stream()
                .sorted(Comparator.comparing(ContestRecordDTO::getTotalScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }
}
