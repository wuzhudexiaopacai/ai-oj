package com.github.wuzhudexiaopacai.oj.log;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 统一日志服务启动类
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.github.wuzhudexiaopacai.oj.log.mapper")
public class LogApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogApplication.class, args);
    }
}
