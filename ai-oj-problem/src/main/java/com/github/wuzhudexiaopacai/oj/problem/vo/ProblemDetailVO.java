package com.github.wuzhudexiaopacai.oj.problem.vo;

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
public class ProblemDetailVO implements Serializable {

    private Long id;

    private String problemKey;

    private String title;

    private String description;

    private String inputDesc;

    private String outputDesc;

    private String hint;

    private String source;

    private Integer difficulty;

    private Integer timeLimit;

    private Integer memoryLimit;

    private Integer stackLimit;

    private Integer acceptCount;

    private Integer submitCount;

    /**
     * 标签列表
     */
    private List<TagVO> tags;

    /**
     * 样例（seq 最小的 2 条）
     */
    private List<SampleCaseVO> samples;

    /**
     * 语言代码模板
     */
    private List<LanguageTemplateVO> templates;
}
