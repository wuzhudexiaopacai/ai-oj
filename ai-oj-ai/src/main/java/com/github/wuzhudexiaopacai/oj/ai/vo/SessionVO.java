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
@Schema(description = "会话视图对象")
public class SessionVO implements Serializable {

    /**
     * 会话 ID
     */
    @Schema(description = "会话 ID")
    private Long id;

    /**
     * 关联题目 ID
     */
    @Schema(description = "关联题目 ID")
    private Long problemId;

    /**
     * 场景编码
     */
    @Schema(description = "场景编码")
    private String scene;

    /**
     * 会话标题
     */
    @Schema(description = "会话标题")
    private String title;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
