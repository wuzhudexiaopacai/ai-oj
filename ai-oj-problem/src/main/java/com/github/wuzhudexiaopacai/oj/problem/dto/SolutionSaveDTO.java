package com.github.wuzhudexiaopacai.oj.problem.dto;

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
public class SolutionSaveDTO implements Serializable {

    /**
     * 题目 ID
     */
    @NotNull(message = "题目 ID 不能为空")
    private Long problemId;

    /**
     * 题解标题
     */
    @NotBlank(message = "题解标题不能为空")
    private String title;

    /**
     * Markdown 内容
     */
    @NotBlank(message = "题解内容不能为空")
    private String content;
}
