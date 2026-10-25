package com.github.wuzhudexiaopacai.oj.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 注册请求
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class RegisterRequest implements Serializable {

    /**
     * 登录名（4-32 位）
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度需为 4-32 位")
    private String username;

    /**
     * 昵称（缺省取用户名）
     */
    @Size(max = 64, message = "昵称长度不能超过 64 位")
    private String nickname;

    /**
     * 密码（6-32 位）
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
    private String password;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 位")
    private String email;

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
