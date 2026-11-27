package com.github.wuzhudexiaopacai.oj.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.core.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Sentinel 网关流控响应配置：路由被限流时返回项目统一 Result 结构
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Configuration
public class GatewaySentinelConfig {

    @Bean
    public BlockRequestHandler sentinelBlockRequestHandler() {
        return (exchange, throwable) -> {
            log.warn("Sentinel 网关限流: path={}, rule={}",
                    exchange.getRequest().getPath(), throwable.getClass().getSimpleName());
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(Result.failed(ResultCode.TOO_MANY_REQUESTS)));
        };
    }
}
