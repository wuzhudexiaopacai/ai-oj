package com.github.wuzhudexiaopacai.oj.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户在指定比赛内的题目通过状态（比赛成绩按 contestId 独立计算）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class ContestProblemStatusDTO implements Serializable {

    /**
     * 该比赛内已通过的题目 ID
     */
    private List<Long> solvedIds = new ArrayList<>();

    /**
     * 该比赛内提交过的题目 ID（含未通过）
     */
    private List<Long> attemptedIds = new ArrayList<>();
}
