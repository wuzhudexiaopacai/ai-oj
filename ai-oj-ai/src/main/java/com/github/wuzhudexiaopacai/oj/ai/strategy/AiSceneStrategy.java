package com.github.wuzhudexiaopacai.oj.ai.strategy;

import java.util.Map;

/**
 * AI 场景策略接口（策略模式：不同场景使用不同的提示词模板与变量）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface AiSceneStrategy {

    /**
     * 场景编码（与 ai_prompt_template.code 一致）
     *
     * @return 场景编码
     */
    String scene();

    /**
     * 基于模板变量渲染完整提示词
     *
     * @param vars 模板变量（如 problem/question、language/code、topic/difficulty）
     * @return 渲染后的提示词
     */
    String buildPrompt(Map<String, String> vars);
}
