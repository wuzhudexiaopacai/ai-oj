package com.github.wuzhudexiaopacai.oj.contest.ranking;

import com.github.wuzhudexiaopacai.oj.api.dto.ContestRecordDTO;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ACM 赛制排序策略：通过题数降序 → 罚时（秒）升序
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Component
public class AcmRankingStrategy implements RankingStrategy {

    @Override
    public List<ContestRecordDTO> sort(List<ContestRecordDTO> records) {
        return records.stream()
                .sorted(Comparator
                        .comparing(ContestRecordDTO::getAcceptCount,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ContestRecordDTO::getPenaltySeconds,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }
}
