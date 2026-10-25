package com.github.wuzhudexiaopacai.oj.contest.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 比赛服务配置（对应 nacos ai-oj-contest.yaml 中 ai-oj.contest 段）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@ConfigurationProperties(prefix = "ai-oj.contest")
public class ContestProperties {

    /**
     * 排行榜 ZSet 缓存 TTL（秒）
     */
    private long rankTtlSeconds = 30;
}
