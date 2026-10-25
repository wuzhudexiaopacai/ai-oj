package com.github.wuzhudexiaopacai.oj.judge.sandbox;

import com.github.wuzhudexiaopacai.oj.judge.sandbox.dto.SandboxJudgeRequest;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.dto.SandboxJudgeResponse;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.dto.SandboxRunRequest;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.dto.SandboxRunResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * 沙箱 HTTP 客户端（WebClient 同步阻塞调用，仅用于沙箱交互）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
public class SandboxClient {

    @Value("${ai-oj.judge.judge-timeout-seconds:120}")
    private long judgeTimeoutSeconds;

    private final WebClient webClient = WebClient.builder().build();

    /**
     * 调用沙箱判题接口
     *
     * @param baseUrl 沙箱地址
     * @param request 判题请求
     * @return 判题响应
     */
    public SandboxJudgeResponse judge(String baseUrl, SandboxJudgeRequest request) {
        return webClient.post()
                .uri(baseUrl + "/judge")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SandboxJudgeResponse.class)
                .timeout(Duration.ofSeconds(judgeTimeoutSeconds))
                .block();
    }

    /**
     * 调用沙箱自测运行接口
     *
     * @param baseUrl 沙箱地址
     * @param request 运行请求
     * @return 运行响应
     */
    public SandboxRunResponse run(String baseUrl, SandboxRunRequest request) {
        return webClient.post()
                .uri(baseUrl + "/run")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SandboxRunResponse.class)
                .timeout(Duration.ofSeconds(judgeTimeoutSeconds))
                .block();
    }
}
