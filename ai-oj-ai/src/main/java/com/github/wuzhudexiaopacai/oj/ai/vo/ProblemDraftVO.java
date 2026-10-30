package com.github.wuzhudexiaopacai.oj.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 出题结果（结构化题目草稿，供前端回填编辑表单）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "AI 出题结果（结构化题目草稿）")
public class ProblemDraftVO implements Serializable {

    /**
     * 题目标题
     */
    @Schema(description = "题目标题", example = "两数之和")
    private String title;

    /**
     * 题目描述（Markdown）
     */
    @Schema(description = "题目描述，支持 Markdown")
    private String description;

    /**
     * 输入描述（Markdown）
     */
    @Schema(description = "输入描述，支持 Markdown")
    private String inputDesc;

    /**
     * 输出描述（Markdown）
     */
    @Schema(description = "输出描述，支持 Markdown")
    private String outputDesc;

    /**
     * 提示（Markdown）
     */
    @Schema(description = "提示/数据范围说明，支持 Markdown")
    private String hint;

    /**
     * 难度：0 简单 / 1 中等 / 2 困难
     */
    @Schema(description = "难度：0 简单 / 1 中等 / 2 困难", example = "1")
    private Integer difficulty;

    /**
     * 时间限制（毫秒）
     */
    @Schema(description = "时间限制（毫秒）", example = "1000")
    private Integer timeLimit;

    /**
     * 内存限制（MB）
     */
    @Schema(description = "内存限制（MB）", example = "256")
    private Integer memoryLimit;

    /**
     * 评测用例
     */
    @Schema(description = "AI 生成的评测用例列表")
    private List<GeneratedCaseVO> testCases;

    /**
     * 题解（Markdown，含思路与参考代码）
     */
    @Schema(description = "题解正文，支持 Markdown（含解题思路与参考代码块）")
    private String solution;

    /**
     * 参考代码（C++ 标准答案，用于用例校验与自测）
     */
    @Schema(description = "C++ 参考代码（完整可编译运行的标准答案，读标准输入写标准输出）")
    private String referenceCode;
}
