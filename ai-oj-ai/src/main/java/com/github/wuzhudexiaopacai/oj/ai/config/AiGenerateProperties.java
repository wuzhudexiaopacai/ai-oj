package com.github.wuzhudexiaopacai.oj.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 出题场景专用模型参数（对应 Nacos ai-oj-ai.yaml 中 ai-oj.generate 段）。
 *
 * <p>出题需一次性输出「题面 + 多组用例 + 完整参考代码 + 题解」的大 JSON，输出量远大于
 * 普通对话。若沿用全局 {@code spring.ai.dashscope.chat.options.max-tokens}（2048），
 * 难题会在 referenceCode 中途被截断，导致 JSON 不完整、解析抛 {@code JsonEOFException}。
 * 故为出题单独放宽 max-tokens，通过 per-request options 覆盖全局默认，互不影响。</p>
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@ConfigurationProperties(prefix = "ai-oj.generate")
public class AiGenerateProperties {

    /**
     * 出题使用的模型名。qwen-plus 稳定版最大输出仅 8192，出题长 JSON 易被截断，
     * 故默认用 qwen-plus-latest（最大输出 16384，适合代码生成/长文档场景）
     */
    private String model = "qwen-plus-latest";

    /**
     * 出题单次生成最大 token 数（需容纳题面+用例+参考代码+题解，故显著大于对话场景）
     */
    private Integer maxTokens = 16384;

    /**
     * 采样温度，范围 [0, 2)
     */
    private Double temperature = 0.7;
}
