package com.github.wuzhudexiaopacai.oj.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DashScope（通义千问）模型连接与调用参数，对应 Nacos 中 spring.ai.dashscope 段。
 *
 * <p>该配置由 Spring Cloud 的 ConfigurationPropertiesRebinder 在 Nacos 推送变更时自动重新绑定，
 * 配合 {@link AiConfig} 中 {@code @RefreshScope} 的模型 Bean 实现密钥/URL/模型名热更新，无需重启服务。</p>
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.dashscope")
public class AiModelProperties {

    /**
     * 百炼/DashScope API Key
     */
    private String apiKey;

    /**
     * 服务端点，默认阿里云 DashScope 原生地址
     */
    private String baseUrl = "https://dashscope.aliyuncs.com";

    /**
     * 业务空间 ID（可选，多空间账号使用）
     */
    private String workSpaceId;

    /**
     * 对话参数
     */
    private Chat chat = new Chat();

    @Data
    public static class Chat {

        /**
         * 调用选项
         */
        private Options options = new Options();

        @Data
        public static class Options {

            /**
             * 模型名称，如 qwen-plus / qwen-max / qwen-turbo
             */
            private String model = "qwen-plus";

            /**
             * 采样温度，范围 [0, 2)
             */
            private Double temperature = 0.7;

            /**
             * 单次生成最大 token 数
             */
            private Integer maxTokens = 2048;
        }
    }
}
