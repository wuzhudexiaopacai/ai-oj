package com.github.wuzhudexiaopacai.oj.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务配置入口（注册配置属性 + ChatClient 全局兜底系统提示词）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    /**
     * 兜底系统提示词（当场景模板缺失时使用）
     */
    private static final String DEFAULT_SYSTEM = "你是 AI-OJ 内置的编程助手，擅长算法与工程问题，用中文简洁回答。";

    /**
     * 构造器注入 ChatClient.Builder，defaultSystem 兜底
     *
     * @param builder 聊天客户端构建器
     * @return 配置完成的聊天客户端
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultSystem(DEFAULT_SYSTEM).build();
    }
}
