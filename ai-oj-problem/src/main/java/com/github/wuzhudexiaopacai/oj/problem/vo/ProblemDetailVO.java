package com.github.wuzhudexiaopacai.oj.problem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 题目详情
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "题目详情")
public class ProblemDetailVO implements Serializable {

    @Schema(description = "题目 ID")
    private Long id;

    @Schema(description = "展示题号，如 A1001")
    private String problemKey;

    @Schema(description = "题目标题")
    private String title;

    @Schema(description = "题目描述，Markdown 原文")
    private String description;

    @Schema(description = "输入说明，可能为 null")
    private String inputDesc;

    @Schema(description = "输出说明，可能为 null")
    private String outputDesc;

    @Schema(description = "提示信息，可能为 null")
    private String hint;

    @Schema(description = "题目来源，无来源时为空字符串")
    private String source;

    @Schema(description = "难度：0简单 1中等 2困难")
    private Integer difficulty;

    @Schema(description = "时间限制，单位毫秒（ms）")
    private Integer timeLimit;

    @Schema(description = "内存限制，单位 MB")
    private Integer memoryLimit;

    @Schema(description = "栈限制，单位 MB")
    private Integer stackLimit;

    @Schema(description = "通过次数（MQ 异步统计，可能为 0）")
    private Integer acceptCount;

    @Schema(description = "提交次数（MQ 异步统计，可能为 0）")
    private Integer submitCount;

    /**
     * 标签列表
     */
    @Schema(description = "标签列表，无标签时为空列表")
    private List<TagVO> tags;

    /**
     * 样例（seq 最小的 2 条）
     */
    @Schema(description = "样例列表（seq 最小的 2 条），可能为空列表")
    private List<SampleCaseVO> samples;

    /**
     * 语言代码模板
     */
    @Schema(description = "语言代码模板列表，可能为空列表")
    private List<LanguageTemplateVO> templates;
}
