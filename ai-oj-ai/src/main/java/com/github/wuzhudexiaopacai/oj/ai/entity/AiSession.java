package com.github.wuzhudexiaopacai.oj.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 会话实体（ai_session）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_session")
public class AiSession extends BaseEntity {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 关联题目（可为空表示通用对话）
     */
    private Long problemId;

    /**
     * 场景：chat/hint/review/generate
     */
    private String scene;

    /**
     * 会话标题
     */
    private String title;
}
