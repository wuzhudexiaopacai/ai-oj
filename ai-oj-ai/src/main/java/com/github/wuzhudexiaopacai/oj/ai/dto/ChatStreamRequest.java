package com.github.wuzhudexiaopacai.oj.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 流式对话请求（用 POST 传代码，避免超长代码超出 URL 长度限制）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "流式对话请求（POST 传输代码，避免 URL 超长）")
public class ChatStreamRequest implements Serializable {

    /**
     * 会话 ID
     */
    @Schema(description = "会话 ID，须为当前登录用户自己的会话，用于确定场景并加载历史记忆", example = "1850000000000000001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "sessionId 不能为空")
    private Long sessionId;

    /**
     * 用户问题
     */
    @Schema(description = "用户问题内容，不能为空；会与题目上下文、当前代码一起渲染进场景提示词", example = "这段代码为什么会超时？",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "问题不能为空")
    private String question;

    /**
     * 用户当前编辑器中的代码（可选，随每次提问实时上报）
     */
    @Schema(description = "用户当前编辑器代码（可选），注入提示词时最多保留 8000 字符，超出部分截断", example = "public class Main { public static void main(String[] args) { ... } }")
    private String code;

    /**
     * 当前代码语言（可选）
     */
    @Schema(description = "当前代码语言（可选），如 java/cpp/python，用于标注提示词代码块", example = "java")
    private String language;
}
