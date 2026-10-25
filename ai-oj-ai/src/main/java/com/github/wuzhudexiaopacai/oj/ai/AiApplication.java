package com.github.wuzhudexiaopacai.oj.ai;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeChatAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智能服务启动类（AI 对话 / 提示 / 代码评审 / 异步出题）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@EnableAsync
@EnableScheduling
// 关闭 DashScope 自动装配：模型由 AiConfig 手动构建并支持 Nacos 热更新
@SpringBootApplication(exclude = DashScopeChatAutoConfiguration.class)
@EnableDiscoveryClient
@MapperScan("com.github.wuzhudexiaopacai.oj.ai.mapper")
@EnableFeignClients(basePackages = "com.github.wuzhudexiaopacai.oj.api.feign")
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
