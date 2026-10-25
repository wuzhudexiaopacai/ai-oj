package com.github.wuzhudexiaopacai.oj.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 文件服务启动类（MinIO 大文件分片上传）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.github.wuzhudexiaopacai.oj.file.mapper")
@EnableScheduling
public class FileApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
    }
}
