package com.github.wuzhudexiaopacai.oj.problem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 题解提交入参
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "题解提交入参")
public class SolutionSaveDTO implements Serializable {

    /**
     * 题目 ID
     */
    @NotNull(message = "题目 ID 不能为空")
    @Schema(description = "题目 ID，题目必须存在；必填", example = "10001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long problemId;

    /**
     * 题解标题
     */
    @NotBlank(message = "题解标题不能为空")
    @Schema(description = "题解标题；必填，不能为空", example = "哈希表一次遍历解法",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    /**
     * Markdown 内容
     */
    @NotBlank(message = "题解内容不能为空")
    @Schema(description = "题解正文，Markdown 文本；必填，不能为空", example = "## 思路\n使用哈希表记录已遍历元素……",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
