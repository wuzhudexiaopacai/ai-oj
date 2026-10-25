package com.github.wuzhudexiaopacai.oj.problem.dto;

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
public class ProblemSaveDTO implements Serializable {

    /**
     * 题目 ID（更新时必填）
     */
    private Long id;

    /**
     * 展示题号，如 A1001
     */
    @NotBlank(message = "题号不能为空")
    private String problemKey;

    /**
     * 题目标题
     */
    @NotBlank(message = "标题不能为空")
    private String title;

    /**
     * 题目描述（Markdown）
     */
    @NotBlank(message = "题目描述不能为空")
    private String description;

    private String inputDesc;

    private String outputDesc;

    private String hint;

    private String source;

    /**
     * 难度：1简单 2中等 3困难
     */
    @NotNull(message = "难度不能为空")
    private Integer difficulty;

    @NotNull(message = "时间限制不能为空")
    private Integer timeLimit;

    @NotNull(message = "内存限制不能为空")
    private Integer memoryLimit;

    private Integer stackLimit;

    /**
     * 是否特殊判题：0否 1是
     */
    private Integer isSpj;

    private String spjCode;

    private String spjLanguage;

    /**
     * 计分模式：0 ACM 1 OI
     */
    private Integer ioScore;

    /**
     * 状态：0下架 1上架 2比赛中
     */
    private Integer status;

    /**
     * 可见性：1公开 2私有 3比赛
     */
    private Integer auth;

    /**
     * 标签 ID 列表
     */
    private List<Long> tagIds;

    /**
     * 评测用例
     */
    @Valid
    private List<TestCaseSaveDTO> testCases;

    /**
     * 语言代码模板
     */
    @Valid
    private List<LanguageTemplateDTO> languageTemplates;
}
