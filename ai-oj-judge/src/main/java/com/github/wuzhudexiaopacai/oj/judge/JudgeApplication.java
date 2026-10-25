package com.github.wuzhudexiaopacai.oj.judge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI-OJ 判题服务启动类
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@EnableDiscoveryClient
@EnableScheduling
@EnableFeignClients(basePackages = "com.github.wuzhudexiaopacai.oj.api.feign")
@MapperScan("com.github.wuzhudexiaopacai.oj.judge.mapper")
@SpringBootApplication
public class JudgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(JudgeApplication.class, args);
    }
}
