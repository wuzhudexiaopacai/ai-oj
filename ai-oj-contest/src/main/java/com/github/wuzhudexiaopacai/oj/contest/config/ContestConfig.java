package com.github.wuzhudexiaopacai.oj.contest.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 比赛服务配置入口（注册配置属性类）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties(ContestProperties.class)
public class ContestConfig {
}
