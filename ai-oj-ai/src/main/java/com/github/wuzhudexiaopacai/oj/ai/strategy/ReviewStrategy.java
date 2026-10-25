package com.github.wuzhudexiaopacai.oj.ai.strategy;

import com.github.wuzhudexiaopacai.oj.ai.constant.AiConstants;
import com.github.wuzhudexiaopacai.oj.ai.service.PromptTemplateService;
import org.springframework.stereotype.Component;

/**
 * 代码评审策略（模板变量：{language}/{code}）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Component
public class ReviewStrategy extends AbstractAiSceneStrategy {

    public ReviewStrategy(PromptTemplateService promptTemplateService) {
        super(promptTemplateService);
    }

    @Override
    public String scene() {
        return AiConstants.SCENE_REVIEW;
    }

    @Override
    protected String[] requiredVars() {
        return new String[]{"language", "code"};
    }
}
