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
    @Schema(description = "模板名称（可选）")
    private String name;

    /**
     * 提示词模板内容（支持 {var} 占位）
     */
    @NotBlank(message = "模板内容不能为空")
    @Schema(description = "提示词模板内容（支持 {var} 占位）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String template;
}
