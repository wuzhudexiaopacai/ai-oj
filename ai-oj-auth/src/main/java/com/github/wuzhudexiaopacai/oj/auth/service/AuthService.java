package com.github.wuzhudexiaopacai.oj.auth.service;

import com.github.wuzhudexiaopacai.oj.auth.dto.LoginRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.RegisterRequest;
import com.github.wuzhudexiaopacai.oj.auth.vo.CaptchaVO;
import com.github.wuzhudexiaopacai.oj.auth.vo.LoginVO;
import com.github.wuzhudexiaopacai.oj.auth.vo.UserInfoVO;

/**
 * 认证服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface AuthService {

    /**
     * 生成图形验证码
     *
     * @return 验证码标识与图片
     */
    CaptchaVO captcha();

    /**
     * 账号密码登录
     *
     * @param request   登录请求
     * @param ip        客户端IP
     * @param userAgent 客户端UA
     * @return 令牌与用户信息
     */
    LoginVO login(LoginRequest request, String ip, String userAgent);

    /**
     * 注册账号
     *
     * @param request   注册请求
     * @param ip        客户端IP
     * @param userAgent 客户端UA
     * @return 新用户信息
     */
    UserInfoVO register(RegisterRequest request, String ip, String userAgent);

    /**
     * 刷新令牌（旧令牌进入黑名单，按当前权限重新签发）
     *
     * @param token 旧令牌（不含 Bearer 前缀）
     * @return 新令牌与用户信息
     */
    LoginVO refresh(String token);

    /**
     * 登出（令牌加入黑名单）
     *
     * @param token 令牌（不含 Bearer 前缀），为空或已失效时幂等返回
     */
    void logout(String token);
}
