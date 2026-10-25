package com.github.wuzhudexiaopacai.oj.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * AI 异步任务消息（RocketMQ 载荷）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskMessage implements Serializable {

    /**
     * 任务 ID
     */
    private String taskId;

    /**
     * 任务类型：generate/review
     */
    private String taskType;

    /**
     * 场景模板变量（topic/difficulty 或 language/code）
     */
    private Map<String, String> params;
}
