package com.github.wuzhudexiaopacai.oj.common.web.feign;

import com.github.wuzhudexiaopacai.oj.common.core.constant.CommonConstants;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * Feign 请求头透传：把网关注入的登录态头接力传给下游服务，并标记内部调用
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Configuration
@ConditionalOnClass(name = "feign.RequestInterceptor")
public class FeignRelayConfig {

    private static final List<String> RELAY_HEADERS = List.of(
            CommonConstants.HEADER_USER_ID,
            CommonConstants.HEADER_USER_NAME,
            CommonConstants.HEADER_AUTHORITIES,
            // 下游服务需要校验网关信任令牌才采信身份头，服务间调用必须一并透传
            CommonConstants.HEADER_GATEWAY_TOKEN
    );

    @Bean
    public RequestInterceptor headerRelayInterceptor() {
        return template -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                for (String header : RELAY_HEADERS) {
                    String value = request.getHeader(header);
                    if (value != null) {
                        template.header(header, value);
                    }
                }
            }
            // 标记为内部调用（异步线程无请求上下文时也会带上）
            template.header(CommonConstants.HEADER_INTERNAL_CALL, "true");
        };
    }
}
