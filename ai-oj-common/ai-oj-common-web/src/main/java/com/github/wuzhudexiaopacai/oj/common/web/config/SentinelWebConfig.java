package com.github.wuzhudexiaopacai.oj.common.web.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.core.result.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

/**
 * Sentinel 限流响应配置：被流控/熔断拦截时返回项目统一 Result 结构
 * <p>
 * 默认实现返回纯文本 "Blocked by Sentinel (flow limiting)"，前端无法按统一错误码处理；
 * 此处改为 HTTP 200 + {@code code=429}，与全局异常、鉴权失败的返回风格保持一致。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Configuration
public class SentinelWebConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Bean
    public BlockExceptionHandler sentinelBlockExceptionHandler() {
        return (request, response, resourceName, blockException) -> {
            log.warn("Sentinel 拦截请求: uri={}, resource={}, rule={}",
                    request.getRequestURI(), resourceName, blockException.getClass().getSimpleName());
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            OBJECT_MAPPER.writeValue(response.getWriter(), Result.failed(ResultCode.TOO_MANY_REQUESTS));
        };
    }
}
