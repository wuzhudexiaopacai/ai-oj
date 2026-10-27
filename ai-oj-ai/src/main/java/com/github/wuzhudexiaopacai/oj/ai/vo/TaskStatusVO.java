package com.github.wuzhudexiaopacai.oj.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 异步任务状态视图对象（同时作为 Redis 缓存结构）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "异步任务状态视图对象（同时作为 Redis 缓存结构）")
public class TaskStatusVO implements Serializable {

    /**
     * 任务 ID
     */
    @Schema(description = "任务 ID（提交任务时生成的 32 位 UUID，无连字符）", example = "e3b0c44298fc1c149afbf4c8996fb92427")
    private String taskId;

    /**
     * 状态：PENDING/SUCCESS/FAILED
     */
    @Schema(description = "任务状态：PENDING 待执行 / SUCCESS 成功 / FAILED 失败", example = "PENDING")
    private String status;

    /**
     * 任务结果（成功时为 LLM 生成内容，失败时为错误信息）
     */
    @Schema(description = "任务结果：SUCCESS 时为 LLM 生成内容，FAILED 时为错误信息；PENDING 时为 null", example = "## 题目解析\n...")
    private String result;
}
