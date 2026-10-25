package com.github.wuzhudexiaopacai.oj.ai.strategy;

import com.github.wuzhudexiaopacai.oj.ai.constant.AiConstants;
import com.github.wuzhudexiaopacai.oj.ai.service.PromptTemplateService;
import org.springframework.stereotype.Component;

/**
 * AI 出题策略（模板变量：{topic}/{difficulty}）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Component
public class GenerateStrategy extends AbstractAiSceneStrategy {

    public GenerateStrategy(PromptTemplateService promptTemplateService) {
        super(promptTemplateService);
    }

    @Override
    public String scene() {
        return AiConstants.SCENE_GENERATE;
    }

    @Override
    protected String[] requiredVars() {
        return new String[]{"topic", "difficulty"};
    }
}
