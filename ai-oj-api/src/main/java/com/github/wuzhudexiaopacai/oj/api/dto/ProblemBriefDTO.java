package com.github.wuzhudexiaopacai.oj.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 题目简要信息（内部 Feign 契约，用于提交记录等场景回填题号/标题）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "题目简要信息（内部 Feign 契约，用于提交记录等场景回填题号与标题）")
public class ProblemBriefDTO implements Serializable {

    @Schema(description = "题目 ID（雪花 ID）", example = "10001")
    private Long problemId;

    @Schema(description = "展示题号", example = "A1001")
    private String problemKey;

    @Schema(description = "题目标题", example = "两数之和")
    private String title;

    /**
     * 难度：0 简单 1 中等 2 困难
     */
    @Schema(description = "难度：0简单 1中等 2困难", example = "0")
    private Integer difficulty;
}
