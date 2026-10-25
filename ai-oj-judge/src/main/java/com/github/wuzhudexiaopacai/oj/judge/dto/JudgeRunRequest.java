package com.github.wuzhudexiaopacai.oj.judge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 自测运行请求（不入库）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class JudgeRunRequest {

    /**
     * 题目 ID（可选，用于取题目限制）
     */
    private Long problemId;

    @NotBlank(message = "language 不能为空")
    private String language;

    @NotBlank(message = "code 不能为空")
    private String code;

    /**
     * 自测输入
     */
    private String input;
}
