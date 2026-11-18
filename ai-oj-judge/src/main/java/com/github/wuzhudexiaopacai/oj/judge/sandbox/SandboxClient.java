package com.github.wuzhudexiaopacai.oj.judge.sandbox;

import cn.hutool.core.util.StrUtil;
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

    /**
     * 沙箱访问令牌请求头
     */
    private static final String TOKEN_HEADER = "X-Sandbox-Token";

    /**
     * 沙箱单次调用超时（秒）：一次判题需编译 + 逐用例运行，
     * 必须独立于「卡死判定阈值」，否则多用例题目会被误杀
     */
    @Value("${ai-oj.judge.sandbox-call-timeout-seconds:300}")
    private long sandboxCallTimeoutSeconds;

    /**
     * 沙箱访问令牌（与沙箱 SANDBOX_API_TOKEN 一致），为空表示沙箱未开启校验
     */
    @Value("${ai-oj.judge.sandbox-token:}")
    private String sandboxToken;

    private final WebClient webClient = WebClient.builder().build();

    /**
     * 调用沙箱判题接口
     *
     * @param baseUrl 沙箱地址
     * @param request 判题请求
     * @return 判题响应
     */
    public SandboxJudgeResponse judge(String baseUrl, SandboxJudgeRequest request) {
        return withToken(webClient.post()
                .uri(baseUrl + "/judge")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request))
                .retrieve()
                .bodyToMono(SandboxJudgeResponse.class)
                .timeout(Duration.ofSeconds(sandboxCallTimeoutSeconds))
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
        return withToken(webClient.post()
                .uri(baseUrl + "/run")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request))
                .retrieve()
                .bodyToMono(SandboxRunResponse.class)
                .timeout(Duration.ofSeconds(sandboxCallTimeoutSeconds))
                .block();
    }

    /**
     * 令牌非空时附加访问令牌请求头
     */
    private WebClient.RequestHeadersSpec<?> withToken(WebClient.RequestHeadersSpec<?> spec) {
        return StrUtil.isBlank(sandboxToken) ? spec : spec.header(TOKEN_HEADER, sandboxToken);
    }
}
