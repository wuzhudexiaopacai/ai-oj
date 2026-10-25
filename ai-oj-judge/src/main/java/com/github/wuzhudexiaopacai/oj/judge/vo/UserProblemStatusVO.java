package com.github.wuzhudexiaopacai.oj.judge.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 当前用户的题目做题状态（供题库列表标记「已通过 / 尝试过」）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class UserProblemStatusVO implements Serializable {

    /**
     * 已通过的题目 ID
     */
    private List<Long> solvedIds = new ArrayList<>();

    /**
     * 提交过的题目 ID（含未通过）
     */
    private List<Long> attemptedIds = new ArrayList<>();
}
