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
@Schema(description = "消息视图对象")
public class MessageVO implements Serializable {

    /**
     * 消息 ID
     */
    @Schema(description = "消息 ID")
    private Long id;

    /**
     * 角色：user/assistant/system
     */
    @Schema(description = "角色")
    private String role;

    /**
     * 内容
     */
    @Schema(description = "内容")
    private String content;

    /**
     * 消耗 token 数
     */
    @Schema(description = "消耗 token 数")
    private Integer tokens;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
