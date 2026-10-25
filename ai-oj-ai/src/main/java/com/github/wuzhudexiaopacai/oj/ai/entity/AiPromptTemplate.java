package com.github.wuzhudexiaopacai.oj.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 提示词模板实体（ai_prompt_template）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_prompt_template")
public class AiPromptTemplate extends BaseEntity {

    /**
     * 模板编码：hint/review/generate/chat
     */
    private String code;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 提示词模板（支持 {var} 占位）
     */
    private String template;

    /**
     * 状态：1 启用 0 停用
     */
    private Integer status;
}
