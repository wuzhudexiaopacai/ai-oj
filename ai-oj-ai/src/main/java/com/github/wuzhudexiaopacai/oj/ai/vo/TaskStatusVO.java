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
@Schema(description = "异步任务状态视图对象")
public class TaskStatusVO implements Serializable {

    /**
     * 任务 ID
     */
    @Schema(description = "任务 ID")
    private String taskId;

    /**
     * 状态：PENDING/SUCCESS/FAILED
     */
    @Schema(description = "状态：PENDING/SUCCESS/FAILED")
    private String status;

    /**
     * 任务结果（成功时为 LLM 生成内容，失败时为错误信息）
     */
    @Schema(description = "任务结果")
    private String result;
}
