package com.github.wuzhudexiaopacai.oj.judge.sandbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * 基于 GET /health 的沙箱健康探测
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
public class HttpSandboxProbe implements SandboxProbe {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient = WebClient.builder().build();

    @Override
    public boolean healthy(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return false;
        }
        try {
            String body = webClient.get()
                    .uri(stripTrailingSlash(baseUrl) + "/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();
            return body != null;
        } catch (Exception e) {
            log.debug("沙箱健康探测失败 baseUrl={} reason={}", baseUrl, e.getMessage());
            return false;
        }
    }

    private String stripTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
