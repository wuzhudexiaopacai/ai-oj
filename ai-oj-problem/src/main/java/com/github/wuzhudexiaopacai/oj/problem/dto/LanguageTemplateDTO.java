package com.github.wuzhudexiaopacai.oj.problem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 语言代码模板入参
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "语言代码模板入参")
public class LanguageTemplateDTO implements Serializable {

    /**
     * 语言：java/cpp/python/go...
     */
    @NotBlank(message = "语言不能为空")
    @Schema(description = "编程语言代码：cpp/c/java/python3/go/javascript；必填，不能为空", example = "cpp",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String language;

    /**
     * 初始代码模板
     */
    @Schema(description = "该语言的初始代码模板，可空", example = "#include <bits/stdc++.h>\nint main() { return 0; }")
    private String codeTemplate;
}
