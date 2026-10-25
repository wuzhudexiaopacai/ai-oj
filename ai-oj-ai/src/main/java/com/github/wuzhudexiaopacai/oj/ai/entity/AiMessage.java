package com.github.wuzhudexiaopacai.oj.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 消息实体（ai_message）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_message")
public class AiMessage extends BaseEntity {

    /**
     * 会话 ID
     */
    private Long sessionId;

    /**
     * 角色：user/assistant/system
     */
    private String role;

    /**
     * 内容
     */
    private String content;

    /**
     * 消耗 token 数
     */
    private Integer tokens;
}
