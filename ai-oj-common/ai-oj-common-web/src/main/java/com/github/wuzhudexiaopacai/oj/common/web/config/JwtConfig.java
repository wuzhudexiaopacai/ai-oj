package com.github.wuzhudexiaopacai.oj.common.web.config;

import com.github.wuzhudexiaopacai.oj.common.core.util.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置（密钥与过期时间由 Nacos 统一下发）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Configuration
public class JwtConfig {

    @Value("${ai-oj.jwt.secret}")
    private String secret;

    @Value("${ai-oj.jwt.expire-minutes:720}")
    private long expireMinutes;

    @Bean
    public JwtUtils jwtUtils() {
        return new JwtUtils(secret, expireMinutes * 60_000L);
    }
}
