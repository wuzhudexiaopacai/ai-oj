package com.github.wuzhudexiaopacai.oj.auth.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.github.wuzhudexiaopacai.oj.auth.dto.LoginRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.RegisterRequest;
import com.github.wuzhudexiaopacai.oj.auth.service.AuthService;
import com.github.wuzhudexiaopacai.oj.auth.vo.CaptchaVO;
import com.github.wuzhudexiaopacai.oj.auth.vo.LoginVO;
import com.github.wuzhudexiaopacai.oj.auth.vo.UserInfoVO;
import com.github.wuzhudexiaopacai.oj.common.core.annotation.Idempotent;
import com.github.wuzhudexiaopacai.oj.common.core.constant.CommonConstants;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（验证码/登录/注册/刷新/登出，网关白名单放行前四项）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 获取图形验证码
     *
     * @return 验证码标识与图片
     */
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.ok(authService.captcha());
    }

    /**
     * 账号密码登录
     *
     * @param loginRequest 登录请求
     * @param request      HTTP 请求（提取 IP/UA 记录登录日志）
     * @return 令牌与用户信息
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        return Result.ok(authService.login(loginRequest, clientIp(request), request.getHeader("User-Agent")));
    }

    /**
     * 注册账号
     *
     * @param registerRequest 注册请求
     * @param request         HTTP 请求
     * @return 新用户信息
     */
    @Idempotent(key = "#registerRequest.username", expire = 3, message = "注册请求处理中，请勿重复提交")
    @PostMapping("/register")
    public Result<UserInfoVO> register(@Validated @RequestBody RegisterRequest registerRequest, HttpServletRequest request) {
        return Result.ok(authService.register(registerRequest, clientIp(request), request.getHeader("User-Agent")));
    }

    /**
     * 刷新令牌（请求头携带旧令牌：Authorization: Bearer {token}）
     *
     * @param request HTTP 请求
     * @return 新令牌与用户信息
     */
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(HttpServletRequest request) {
        return Result.ok(authService.refresh(resolveToken(request)));
    }

    /**
     * 登出（令牌加入黑名单）
     *
     * @param request HTTP 请求
     * @return 空
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        authService.logout(resolveToken(request));
        return Result.ok();
    }

    /**
     * 提取客户端 IP（经网关转发时取 X-Forwarded-For）
     */
    private String clientIp(HttpServletRequest request) {
        return JakartaServletUtil.getClientIP(request);
    }

    /**
     * 从 Authorization 头解析裸令牌
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(CommonConstants.TOKEN_HEADER);
        if (StrUtil.isNotBlank(header) && header.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return header.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
}
