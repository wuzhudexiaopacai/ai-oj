package com.github.wuzhudexiaopacai.oj.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 服务配置属性（对应 Nacos ai-oj-ai.yaml 中 ai-oj.ai 段）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@ConfigurationProperties(prefix = "ai-oj.ai")
public class AiProperties {

    /**
     * 每用户每分钟 AI 调用限流（Redisson RateLimiter）
     */
    private int rateLimitPerMinute = 10;

    /**
     * 会话记忆最大轮数（超出截断）
     */
    private int maxMemoryRounds = 20;
}
