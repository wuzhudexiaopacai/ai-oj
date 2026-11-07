package com.github.wuzhudexiaopacai.oj.common.web.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.github.wuzhudexiaopacai.oj.common.web.aspect.IdempotentAspect;
import com.github.wuzhudexiaopacai.oj.common.web.aspect.OperationLogAspect;
import com.github.wuzhudexiaopacai.oj.common.web.feign.FeignRelayConfig;
import com.github.wuzhudexiaopacai.oj.common.web.handler.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigInteger;

/**
 * 通用 Web 自动装配入口（业务服务引入 common-web 后零配置生效）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@AutoConfiguration
@Import({MybatisPlusConfig.class, RedisConfig.class, RedissonConfig.class, JwtConfig.class,
        DefaultSecurityConfig.class, IdempotentAspect.class, OperationLogAspect.class,
        GlobalExceptionHandler.class, FeignRelayConfig.class})
public class CommonWebAutoConfiguration {

    /**
     * HTTP 响应中 Long/BigInteger 序列化为字符串，避免雪花 ID 超出 JS Number 安全范围导致前端精度丢失
     * （仅作用于 Spring MVC 的 ObjectMapper；Redis 使用独立的 ObjectMapper，不受影响）
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> builder
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance)
                .serializerByType(BigInteger.class, ToStringSerializer.instance);
    }
}
