package com.github.wuzhudexiaopacai.oj.problem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 语言代码模板展示对象
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "语言代码模板展示对象")
public class LanguageTemplateVO implements Serializable {

    @Schema(description = "编程语言代码：cpp/c/java/python3/go/javascript")
    private String language;

    @Schema(description = "该语言的初始代码模板，可能为 null 或空字符串")
    private String codeTemplate;
}
