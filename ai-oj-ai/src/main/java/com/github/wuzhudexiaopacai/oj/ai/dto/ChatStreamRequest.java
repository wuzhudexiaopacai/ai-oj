package com.github.wuzhudexiaopacai.oj.ai.dto;

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
public class ChatStreamRequest implements Serializable {

    /**
     * 会话 ID
     */
    @NotNull(message = "sessionId 不能为空")
    private Long sessionId;

    /**
     * 用户问题
     */
    @NotBlank(message = "问题不能为空")
    private String question;

    /**
     * 用户当前编辑器中的代码（可选，随每次提问实时上报）
     */
    private String code;

    /**
     * 当前代码语言（可选）
     */
    private String language;
}
