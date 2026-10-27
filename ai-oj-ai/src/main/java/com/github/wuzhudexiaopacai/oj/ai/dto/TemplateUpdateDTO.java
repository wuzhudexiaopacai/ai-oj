package com.github.wuzhudexiaopacai.oj.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 提示词模板更新请求
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "提示词模板更新请求")
public class TemplateUpdateDTO implements Serializable {

    /**
     * 模板名称（可选）
     */
    @Schema(description = "模板名称（可选），为空时保留数据库中的原名称", example = "解题提示")
    private String name;

    /**
     * 提示词模板内容（支持 {var} 占位）
     */
    @NotBlank(message = "模板内容不能为空")
    @Schema(description = "提示词模板内容，支持 {var} 占位；变量随模板编码不同（hint: {problem}/{question}，review: {language}/{code}，generate: {topic}/{difficulty}，chat: {problem}/{code}/{question}）",
            example = "请结合题目「{problem}」回答用户问题：{question}", requiredMode = Schema.RequiredMode.REQUIRED)
    private String template;
}
