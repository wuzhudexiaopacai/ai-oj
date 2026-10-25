package com.github.wuzhudexiaopacai.oj.judge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 判题提交请求
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class JudgeSubmitRequest {

    /**
     * 幂等提交标识（客户端生成）
     */
    @NotBlank(message = "submitId 不能为空")
    private String submitId;

    @NotNull(message = "problemId 不能为空")
    private Long problemId;

    @NotBlank(message = "language 不能为空")
    private String language;

    @NotBlank(message = "code 不能为空")
    private String code;

    /**
     * 比赛 ID（非比赛提交不传，默认 0）
     */
    private Long contestId;
}
