package com.github.wuzhudexiaopacai.oj.ai.strategy;

import com.github.wuzhudexiaopacai.oj.ai.service.PromptTemplateService;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 场景策略抽象基类（模板加载 + {var} 占位渲染的通用实现）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public abstract class AbstractAiSceneStrategy implements AiSceneStrategy {

    /**
     * 模板占位符正则：{var}
     */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");

    protected final PromptTemplateService promptTemplateService;

    protected AbstractAiSceneStrategy(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    /**
     * 当前场景必需的模板变量名
     *
     * @return 变量名数组
     */
    protected abstract String[] requiredVars();

    @Override
    public String buildPrompt(Map<String, String> vars) {
        String template = promptTemplateService.getTemplate(scene());
        for (String var : requiredVars()) {
            Asserts.isTrue(vars != null && vars.get(var) != null && !vars.get(var).isBlank(),
                    "场景变量缺失: " + var);
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String value = vars.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
