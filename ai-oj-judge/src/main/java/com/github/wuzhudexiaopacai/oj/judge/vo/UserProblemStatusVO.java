package com.github.wuzhudexiaopacai.oj.judge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "当前用户题目做题状态（题库列表标记用）")
public class UserProblemStatusVO implements Serializable {

    /**
     * 已通过的题目 ID
     */
    @Schema(description = "已通过（状态 3 答案正确）的题目 ID 列表；无记录时为空列表")
    private List<Long> solvedIds = new ArrayList<>();

    /**
     * 提交过的题目 ID（含未通过）
     */
    @Schema(description = "提交过的题目 ID 列表（含未通过）；无记录时为空列表")
    private List<Long> attemptedIds = new ArrayList<>();
}
