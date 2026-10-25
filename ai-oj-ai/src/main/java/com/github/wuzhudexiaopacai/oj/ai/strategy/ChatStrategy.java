package com.github.wuzhudexiaopacai.oj.ai.strategy;

import com.github.wuzhudexiaopacai.oj.ai.constant.AiConstants;
import com.github.wuzhudexiaopacai.oj.ai.service.PromptTemplateService;
import org.springframework.stereotype.Component;

/**
 * 自由对话策略（模板变量：{question}）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Component
public class ChatStrategy extends AbstractAiSceneStrategy {

    public ChatStrategy(PromptTemplateService promptTemplateService) {
        super(promptTemplateService);
    }

    @Override
    public String scene() {
        return AiConstants.SCENE_CHAT;
    }

    @Override
    protected String[] requiredVars() {
        return new String[]{"question"};
    }
}
