package com.github.wuzhudexiaopacai.oj.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 判题所需的题目信息（Feign 契约）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "判题所需的题目信息（内部 Feign 契约，含评测用例与 SPJ 配置）")
public class ProblemJudgeDTO implements Serializable {

    @Schema(description = "题目 ID（雪花 ID）", example = "10001")
    private Long problemId;

    @Schema(description = "展示题号", example = "A1001")
    private String problemKey;

    @Schema(description = "题目标题", example = "两数之和")
    private String title;

    /**
     * 题目描述（Markdown，供 AI 助手理解题意）
     */
    @Schema(description = "题目描述（Markdown 原文，供 AI 助手理解题意）", example = "给定两个整数，求它们的和。")
    private String description;

    /**
     * 输入说明
     */
    @Schema(description = "输入格式说明", example = "一行两个整数 a b")
    private String inputDesc;

    /**
     * 输出说明
     */
    @Schema(description = "输出格式说明", example = "一行一个整数，表示 a + b")
    private String outputDesc;

    /**
     * 时间限制（毫秒）
     */
    @Schema(description = "时间限制（毫秒），会按语言系数放大", example = "1000")
    private Integer timeLimit;

    /**
     * 内存限制（MB）
     */
    @Schema(description = "内存限制（MB），会按语言系数放大", example = "256")
    private Integer memoryLimit;

    /**
     * 是否 Special Judge
     */
    @Schema(description = "是否特殊判题（Special Judge）", example = "false")
    private Boolean isSpj;

    @Schema(description = "SPJ 判题代码语言，仅 isSpj=true 时有值", example = "cpp")
    private String spjLanguage;

    @Schema(description = "SPJ 判题代码，仅 isSpj=true 时有值")
    private String spjCode;

    /**
     * 评测用例（按 seq 升序）
     */
    @Schema(description = "评测用例列表，按 seq 升序执行")
    private List<JudgeCaseDTO> cases;
}
