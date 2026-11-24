package com.github.wuzhudexiaopacai.oj.common.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.core.result.ResultCode;
import com.github.wuzhudexiaopacai.oj.common.web.filter.HeaderAuthFilter;
import com.github.wuzhudexiaopacai.oj.common.web.filter.InternalCallGuardFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

/**
 * 默认安全配置（各业务服务通用）：
 * HTTP 层放开（公网入口由网关统一鉴权），服务内部用 @PreAuthorize 做 RBAC 细粒度控制。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class DefaultSecurityConfig {

    private static final String[] WHITE_LIST = {
            "/v3/api-docs/**", "/doc.html", "/webjars/**", "/favicon.ico",
            "/error", "/actuator/**", "/ws/**", "/internal/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public HeaderAuthFilter headerAuthFilter() {
        return new HeaderAuthFilter();
    }

    /**
     * 内部接口守卫：/internal/** 免登录，但必须携带内部调用标识头
     */
    @Bean
    public FilterRegistrationBean<InternalCallGuardFilter> internalCallGuardFilter() {
        FilterRegistrationBean<InternalCallGuardFilter> registration =
                new FilterRegistrationBean<>(new InternalCallGuardFilter());
        registration.addUrlPatterns("/internal/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, HeaderAuthFilter headerAuthFilter) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST).permitAll()
                        .anyRequest().permitAll())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, objectMapper, ResultCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, objectMapper, ResultCode.FORBIDDEN)))
                .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeError(HttpServletResponse response, ObjectMapper objectMapper, ResultCode resultCode) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.failed(resultCode)));
    }
}
