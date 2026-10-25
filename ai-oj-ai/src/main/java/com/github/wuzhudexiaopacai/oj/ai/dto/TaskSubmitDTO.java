package com.github.wuzhudexiaopacai.oj.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 异步任务提交请求
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "异步任务提交请求")
public class TaskSubmitDTO implements Serializable {

    /**
     * 任务类型：generate 出题 / review 代码评审
     */
    @NotBlank(message = "任务类型不能为空")
    @Schema(description = "任务类型：generate 出题 / review 代码评审", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskType;

    /**
     * 知识点（generate 必填）
     */
    @Schema(description = "知识点（generate 必填）")
    private String topic;

    /**
     * 难度（generate 可选，默认中等）
     */
    @Schema(description = "难度（generate 可选，默认中等）")
    private String difficulty;

    /**
     * 编程语言（review 必填）
     */
    @Schema(description = "编程语言（review 必填）")
    private String language;

    /**
     * 代码内容（review 必填）
     */
    @Schema(description = "代码内容（review 必填）")
    private String code;
}
