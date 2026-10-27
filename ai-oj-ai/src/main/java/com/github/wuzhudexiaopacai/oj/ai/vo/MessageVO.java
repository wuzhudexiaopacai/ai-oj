package com.github.wuzhudexiaopacai.oj.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息视图对象
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "AI 消息视图对象（会话消息列表返回，按消息 ID 升序）")
public class MessageVO implements Serializable {

    /**
     * 消息 ID
     */
    @Schema(description = "消息 ID（雪花 ID，JSON 序列化为字符串）", example = "1850000000000000001")
    private Long id;

    /**
     * 角色：user/assistant/system
     */
    @Schema(description = "消息角色：user 用户提问 / assistant AI 回答", example = "user")
    private String role;

    /**
     * 内容
     */
    @Schema(description = "消息文本内容；流式回答结束后异步落库，可能稍有延迟", example = "这段代码的时间复杂度是多少？")
    private String content;

    /**
     * 消耗 token 数
     */
    @Schema(description = "消耗 token 数；当前落库固定为 0（预留字段）", example = "0")
    private Integer tokens;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间（ISO-8601）", example = "2026-10-27T22:37:00")
    private LocalDateTime createTime;
}
