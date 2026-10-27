package com.github.wuzhudexiaopacai.oj.problem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 题目保存入参（题目 + 用例 + 语言模板 + 标签一体保存）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "题目保存入参（题目 + 用例 + 语言模板 + 标签一体保存）")
public class ProblemSaveDTO implements Serializable {

    /**
     * 题目 ID（更新时必填）
     */
    @Schema(description = "题目 ID，新增时为空，更新时由路径参数指定", example = "10001")
    private Long id;

    /**
     * 展示题号，如 A1001
     */
    @NotBlank(message = "题号不能为空")
    @Schema(description = "展示题号，全局唯一，如 A1001；必填，不能为空", example = "A1001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String problemKey;

    /**
     * 题目标题
     */
    @NotBlank(message = "标题不能为空")
    @Schema(description = "题目标题；必填，不能为空", example = "两数之和",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    /**
     * 题目描述（Markdown）
     */
    @NotBlank(message = "题目描述不能为空")
    @Schema(description = "题目描述，Markdown 文本；必填，不能为空", example = "给定两个整数，输出它们的和。",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    /**
     * 输入描述
     */
    @Schema(description = "输入说明，可空", example = "一行两个整数 a 和 b")
    private String inputDesc;

    /**
     * 输出描述
     */
    @Schema(description = "输出说明，可空", example = "输出 a + b 的结果")
    private String outputDesc;

    /**
     * 提示
     */
    @Schema(description = "提示信息，可空，如数据范围说明", example = "-10^9 <= a, b <= 10^9")
    private String hint;

    /**
     * 来源
     */
    @Schema(description = "题目来源，可空", example = "LeetCode")
    private String source;

    /**
     * 难度：1简单 2中等 3困难
     */
    @NotNull(message = "难度不能为空")
    @Schema(description = "难度：0简单 1中等 2困难；必填", example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer difficulty;

    /**
     * 时间限制（ms）
     */
    @NotNull(message = "时间限制不能为空")
    @Schema(description = "时间限制，单位毫秒（ms）；必填", example = "1000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer timeLimit;

    /**
     * 内存限制（MB）
     */
    @NotNull(message = "内存限制不能为空")
    @Schema(description = "内存限制，单位 MB；必填", example = "256",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer memoryLimit;

    /**
     * 栈限制（MB）
     */
    @Schema(description = "栈限制，单位 MB，可空", example = "128")
    private Integer stackLimit;

    /**
     * 是否特殊判题：0否 1是
     */
    @Schema(description = "是否特殊判题：0否 1是，不传默认 0", example = "0")
    private Integer isSpj;

    /**
     * SPJ 判题代码
     */
    @Schema(description = "特殊判题代码，isSpj=1 时使用，可空", example = "int main() { /* 比较用户输出与标准输出 */ }")
    private String spjCode;

    /**
     * SPJ 语言
     */
    @Schema(description = "特殊判题代码所用语言，如 cpp/java/python3，isSpj=1 时使用，可空", example = "cpp")
    private String spjLanguage;

    /**
     * 计分模式：0 ACM 1 OI
     */
    @Schema(description = "计分模式：0 ACM（全对才得分） 1 OI（按测试点给分），不传默认 0", example = "0")
    private Integer ioScore;

    /**
     * 状态：0下架 1上架 2比赛中
     */
    @Schema(description = "状态：0下架 1上架 2比赛中；新增不传默认 1上架", example = "1")
    private Integer status;

    /**
     * 可见性：1公开 2私有 3比赛
     */
    @Schema(description = "可见性：1公开 2私有 3比赛；新增不传默认 1公开", example = "1")
    private Integer auth;

    /**
     * 标签 ID 列表
     */
    @Schema(description = "标签 ID 列表，更新时为整体替换；不传则保留原标签", example = "[1,2]")
    private List<Long> tagIds;

    /**
     * 评测用例
     */
    @Valid
    @Schema(description = "评测用例列表，更新时为整体替换；不传则保留原用例",
            example = "[{\"seq\":1,\"inputData\":\"1 2\",\"outputData\":\"3\"}]")
    private List<TestCaseSaveDTO> testCases;

    /**
     * 语言代码模板
     */
    @Valid
    @Schema(description = "多语言代码模板列表，更新时为整体替换；不传则保留原模板",
            example = "[{\"language\":\"cpp\",\"codeTemplate\":\"#include <bits/stdc++.h>\"}]")
    private List<LanguageTemplateDTO> languageTemplates;
}
