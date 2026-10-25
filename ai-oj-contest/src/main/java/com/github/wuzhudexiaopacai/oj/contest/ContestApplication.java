package com.github.wuzhudexiaopacai.oj.contest;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 比赛服务启动类
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.github.wuzhudexiaopacai.oj.contest.mapper")
@EnableFeignClients(basePackages = "com.github.wuzhudexiaopacai.oj.api.feign")
@EnableScheduling
@EnableAsync
public class ContestApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContestApplication.class, args);
    }
}
