package com.github.wuzhudexiaopacai.oj.problem.dto;

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
public class LanguageTemplateDTO implements Serializable {

    /**
     * 语言：java/cpp/python/go...
     */
    @NotBlank(message = "语言不能为空")
    private String language;

    /**
     * 初始代码模板
     */
    private String codeTemplate;
}
