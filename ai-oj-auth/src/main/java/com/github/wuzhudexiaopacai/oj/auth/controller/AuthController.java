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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "认证接口", description = "验证码、登录、注册、令牌刷新与登出；验证码/登录/注册/刷新在网关白名单放行，无需登录，登出需携带令牌")
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
    @Operation(summary = "获取图形验证码",
            description = "生成 4 位字符的图形验证码并写入 Redis，有效期 5 分钟且一次性消费。返回 captchaKey 与 Base64 图片（data:image 前缀），登录/注册时需回传 captchaKey 及用户输入的验证码。无需登录。")
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
    @Operation(summary = "账号密码登录",
            description = "先校验图形验证码（不区分大小写），再比对用户名与密码，账号被禁用时拒绝登录。登录成功签发 JWT（有效期默认 720 分钟，权限为角色编码与权限标识之和），并异步记录登录日志。返回令牌与用户信息，无需登录。")
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
    @Operation(summary = "注册账号",
            description = "校验图形验证码，用户名与邮箱需全局唯一；昵称缺省取用户名，Rating 初始 1500，并自动绑定默认角色 ROLE_USER（角色不存在则跳过）。接口按用户名做 3 秒幂等控制，返回新用户信息，无需登录。")
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
    @Operation(summary = "刷新令牌",
            description = "从请求头 Authorization: Bearer {token} 读取旧令牌，校验其未过期且不在黑名单，且用户仍存在并处于启用状态。刷新后旧令牌立即加入黑名单（保留时长等于令牌有效期），并按数据库当前角色与权限重新签发令牌。需要登录。")
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
    @Operation(summary = "登出",
            description = "将请求头 Authorization 中的当前令牌加入黑名单，使其在剩余有效期内立即失效。令牌为空或已失效时直接返回成功，接口幂等。需要登录。")
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
