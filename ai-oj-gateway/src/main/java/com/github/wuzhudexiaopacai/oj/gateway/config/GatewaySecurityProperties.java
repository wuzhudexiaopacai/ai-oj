package com.github.wuzhudexiaopacai.oj.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关安全配置
 * <p>
 * 注意：白名单在 Nacos 中是 YAML 数组，@Value 无法绑定数组，
 * 必须用 ConfigurationProperties 才能正确解析（并支持 Nacos 热刷新重绑定）。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-oj.security")
public class GatewaySecurityProperties {

    /**
     * 无需认证即可访问的 Ant 风格路径列表
     */
    private List<String> whiteList = new ArrayList<>();
}
