package com.github.wuzhudexiaopacai.oj.common.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.common.core.constant.CommonConstants;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.core.result.ResultCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 内部接口守卫过滤器：/internal/** 仅允许携带内部调用标识头的请求访问
 * <p>
 * 网关会剥离外部请求伪造的 {@code X-Internal-Call} 头，服务间 Feign 调用由
 * {@link com.github.wuzhudexiaopacai.oj.common.web.feign.FeignRelayConfig} 统一注入该头。
 * 即使业务服务端口被直连暴露，缺少标识头的请求也会被拒绝，避免免登录的内部接口被越权调用。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public class InternalCallGuardFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"true".equals(request.getHeader(CommonConstants.HEADER_INTERNAL_CALL))) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            OBJECT_MAPPER.writeValue(response.getWriter(), Result.failed(ResultCode.FORBIDDEN));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
