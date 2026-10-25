package com.github.wuzhudexiaopacai.oj.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件上传配置（对应 nacos ai-oj-file.yaml 中 ai-oj.file 段）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-oj.file")
public class FileUploadProperties {

    /**
     * 分片大小（字节），默认 5MB（MinIO/S3 协议最小分片）
     */
    private long chunkSize = 5242880L;

    /**
     * 预签名 URL 过期时间（秒）
     */
    private long presignExpireSeconds = 600L;

    /**
     * 未完成分片保留时长（分钟），超时由延时消息兜底清理
     */
    private long chunkExpireMinutes = 30L;

    /**
     * 清理延时消息级别（RocketMQ 预设：1s 5s 10s 30s 1m 2m ... 对应 level 1-18）
     */
    private int cleanDelayLevel = 10;
}
