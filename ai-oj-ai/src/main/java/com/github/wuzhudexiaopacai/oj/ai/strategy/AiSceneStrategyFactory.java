package com.github.wuzhudexiaopacai.oj.ai.strategy;

import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI 场景策略工厂（按场景编码路由到具体策略）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Component
public class AiSceneStrategyFactory {

    private final Map<String, AiSceneStrategy> strategyMap;

    public AiSceneStrategyFactory(List<AiSceneStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(AiSceneStrategy::scene, Function.identity()));
    }

    /**
     * 获取场景策略
     *
     * @param scene 场景编码
     * @return 场景策略
     */
    public AiSceneStrategy getStrategy(String scene) {
        AiSceneStrategy strategy = scene == null ? null : strategyMap.get(scene);
        Asserts.notNull(strategy, "不支持的 AI 场景: " + scene);
        return strategy;
    }

    /**
     * 是否支持指定场景
     *
     * @param scene 场景编码
     * @return true 支持
     */
    public boolean supports(String scene) {
        return scene != null && strategyMap.containsKey(scene);
    }
}
