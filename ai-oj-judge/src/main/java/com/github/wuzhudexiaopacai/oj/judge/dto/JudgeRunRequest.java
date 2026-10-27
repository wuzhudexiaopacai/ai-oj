package com.github.wuzhudexiaopacai.oj.judge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 自测运行请求（不入库）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "自测运行请求（不入库，不产生提交记录）")
public class JudgeRunRequest {

    /**
     * 题目 ID（可选，用于取题目限制）
     */
    @Schema(description = "题目 ID（可选，预留用于读取题目限时/内存限制；当前自测运行统一按 10 秒超时执行）",
            example = "1001")
    private Long problemId;

    @NotBlank(message = "language 不能为空")
    @Schema(description = "编程语言，不能为空：cpp/C++17、c/C11、java/Java17、python3/Python3、go、javascript",
            example = "python3", requiredMode = Schema.RequiredMode.REQUIRED)
    private String language;

    @NotBlank(message = "code 不能为空")
    @Schema(description = "自测代码，不能为空，直接提交沙箱编译运行", example = "print(1)",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    /**
     * 自测输入
     */
    @Schema(description = "标准输入内容，可为空；按原文透传给沙箱", example = "1 2")
    private String input;
}
