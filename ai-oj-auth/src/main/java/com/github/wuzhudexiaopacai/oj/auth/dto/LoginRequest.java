package com.github.wuzhudexiaopacai.oj.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class LoginRequest implements Serializable {

    /**
     * 登录名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 验证码标识（/auth/captcha 下发）
     */
    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    /**
     * 图形验证码答案
     */
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}
