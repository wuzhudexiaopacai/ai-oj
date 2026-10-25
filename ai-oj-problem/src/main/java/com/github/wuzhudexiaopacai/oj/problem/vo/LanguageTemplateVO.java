package com.github.wuzhudexiaopacai.oj.problem.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 语言代码模板展示对象
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class LanguageTemplateVO implements Serializable {

    private String language;

    private String codeTemplate;
}
