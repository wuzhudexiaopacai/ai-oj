package com.github.wuzhudexiaopacai.oj.ai.strategy;

import com.github.wuzhudexiaopacai.oj.ai.constant.AiConstants;
import com.github.wuzhudexiaopacai.oj.ai.service.PromptTemplateService;
import org.springframework.stereotype.Component;

/**
 * 解题提示策略（模板变量：{problem}/{question}）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Component
public class HintStrategy extends AbstractAiSceneStrategy {

    public HintStrategy(PromptTemplateService promptTemplateService) {
        super(promptTemplateService);
    }

    @Override
    public String scene() {
        return AiConstants.SCENE_HINT;
    }

    @Override
    protected String[] requiredVars() {
        return new String[]{"problem", "question"};
    }
}
