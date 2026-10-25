package com.github.wuzhudexiaopacai.oj.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建会话请求
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "创建会话请求")
public class SessionCreateDTO implements Serializable {

    /**
     * 场景：chat/hint/review/generate
     */
    @NotBlank(message = "场景不能为空")
    @Schema(description = "场景编码：chat/hint/review/generate", requiredMode = Schema.RequiredMode.REQUIRED)
    private String scene;

    /**
     * 关联题目（可为空）
     */
    @Schema(description = "关联题目 ID（可为空）")
    private Long problemId;

    /**
     * 会话标题（可空，默认按场景生成）
     */
    @Size(max = 128, message = "会话标题长度不能超过 128")
    @Schema(description = "会话标题")
    private String title;
}
