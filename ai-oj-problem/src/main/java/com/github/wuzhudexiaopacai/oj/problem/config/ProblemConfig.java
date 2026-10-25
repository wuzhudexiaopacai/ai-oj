package com.github.wuzhudexiaopacai.oj.problem.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 题库服务配置入口（注册缓存配置属性类）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties(ProblemCacheProperties.class)
public class ProblemConfig {
}
