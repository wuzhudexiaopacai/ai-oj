package com.github.wuzhudexiaopacai.oj.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话视图对象
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "AI 会话视图对象（我的会话分页返回）")
public class SessionVO implements Serializable {

    /**
     * 会话 ID
     */
    @Schema(description = "会话 ID（雪花 ID，JSON 序列化为字符串）", example = "1850000000000000001")
    private Long id;

    /**
     * 关联题目 ID
     */
    @Schema(description = "关联题目 ID（雪花 ID，JSON 序列化为字符串）；为 null 表示通用对话", example = "1001")
    private Long problemId;

    /**
     * 场景编码
     */
    @Schema(description = "场景编码：chat 自由对话 / hint 解题提示 / review 代码评审 / generate AI 出题", example = "hint")
    private String scene;

    /**
     * 会话标题
     */
    @Schema(description = "会话标题；创建时未指定则按场景生成默认标题", example = "解题提示会话")
    private String title;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间（ISO-8601，如 2026-10-27T22:37:00）", example = "2026-10-27T22:37:00")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "最后更新时间（ISO-8601）；分页结果按该字段倒序", example = "2026-10-27T22:40:00")
    private LocalDateTime updateTime;
}
