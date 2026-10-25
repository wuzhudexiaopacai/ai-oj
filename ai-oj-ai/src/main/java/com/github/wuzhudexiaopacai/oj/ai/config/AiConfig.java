package com.github.wuzhudexiaopacai.oj.ai.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务配置入口（Spring AI Alibaba / DashScope 模型装配 + 全局兜底系统提示词）。
 *
 * <p>模型 Bean 标注 {@code @RefreshScope}：Nacos 中 {@code spring.ai.dashscope.*} 变更后，
 * Spring Cloud 会重新绑定 {@link AiModelProperties} 并销毁重建该 Bean，
 * 从而做到密钥、端点、模型名热更新，无需重启服务。</p>
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({AiProperties.class, AiModelProperties.class})
public class AiConfig {

    /**
     * 兜底系统提示词（当场景模板缺失时使用）
     */
    private static final String DEFAULT_SYSTEM = "你是 AI-OJ 内置的编程助手，擅长算法与工程问题，用中文简洁回答。";

    /**
     * 构建 DashScope 聊天模型（每次配置刷新时重建，使新密钥/端点立即生效）
     *
     * @param properties 模型配置
     * @return 聊天模型
     */
    @Bean
    @RefreshScope
    public ChatModel dashScopeChatModel(AiModelProperties properties) {
        DashScopeApi api = DashScopeApi.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .workSpaceId(properties.getWorkSpaceId())
                .build();
        AiModelProperties.Chat.Options options = properties.getChat().getOptions();
        DashScopeChatOptions chatOptions = DashScopeChatOptions.builder()
                .withModel(options.getModel())
                .withTemperature(options.getTemperature())
                .withMaxToken(options.getMaxTokens())
                .build();
        log.info("初始化 DashScope 模型: baseUrl={}, model={}", properties.getBaseUrl(), options.getModel());
        return DashScopeChatModel.builder()
                .dashScopeApi(api)
                .defaultOptions(chatOptions)
                .build();
    }

    /**
     * 聊天客户端（注入的是 RefreshScope 代理，模型重建后自动指向新实例）
     *
     * @param chatModel 聊天模型
     * @return 配置完成的聊天客户端
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).defaultSystem(DEFAULT_SYSTEM).build();
    }
}
