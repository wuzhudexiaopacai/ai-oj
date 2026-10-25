package com.github.wuzhudexiaopacai.oj.problem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 题库服务启动类
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.github.wuzhudexiaopacai.oj.problem.mapper")
@EnableFeignClients(basePackages = "com.github.wuzhudexiaopacai.oj.api.feign")
public class ProblemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProblemApplication.class, args);
    }
}
