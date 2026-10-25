package com.github.wuzhudexiaopacai.oj.problem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 题库缓存配置（对应 nacos ai-oj-problem.yaml 中 ai-oj.problem.cache 段）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@ConfigurationProperties(prefix = "ai-oj.problem.cache")
public class ProblemCacheProperties {

    /**
     * 题目详情缓存基础 TTL（秒），实际写入时叠加 0~300s 随机值防雪崩
     */
    private long detailTtlSeconds = 1800;

    /**
     * 题目列表缓存 TTL（秒）
     */
    private long listTtlSeconds = 300;

    /**
     * 空值缓存 TTL（秒，防穿透）
     */
    private long nullTtlSeconds = 60;
}
