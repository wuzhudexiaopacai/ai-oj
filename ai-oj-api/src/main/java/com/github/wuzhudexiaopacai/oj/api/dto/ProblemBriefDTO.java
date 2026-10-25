package com.github.wuzhudexiaopacai.oj.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 题目简要信息（内部 Feign 契约，用于提交记录等场景回填题号/标题）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class ProblemBriefDTO implements Serializable {

    private Long problemId;

    private String problemKey;

    private String title;

    /**
     * 难度：0 简单 1 中等 2 困难
     */
    private Integer difficulty;
}
