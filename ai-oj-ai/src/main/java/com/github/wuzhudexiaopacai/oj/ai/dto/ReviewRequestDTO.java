package com.github.wuzhudexiaopacai.oj.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 代码评审请求
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "代码评审请求")
public class ReviewRequestDTO implements Serializable {

    /**
     * 编程语言
     */
    @NotBlank(message = "编程语言不能为空")
    @Schema(description = "编程语言，如 java/cpp/python，用于渲染评审提示词", example = "java",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String language;

    /**
     * 代码内容
     */
    @NotBlank(message = "代码不能为空")
    @Schema(description = "待评审的完整源码文本，原样注入评审提示词", example = "public class Main { public static void main(String[] args) { System.out.println(1); } }",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}
