package com.github.wuzhudexiaopacai.oj.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * AI-OJ 认证中心启动类
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@EnableDiscoveryClient
@MapperScan("com.github.wuzhudexiaopacai.oj.auth.mapper")
@EnableFeignClients(basePackages = "com.github.wuzhudexiaopacai.oj.api.feign")
@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
