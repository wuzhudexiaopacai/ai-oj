package com.github.wuzhudexiaopacai.oj.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "注册请求")
public class RegisterRequest implements Serializable {

    /**
     * 登录名（4-32 位）
     */
    @Schema(description = "登录名，长度 4-32 位，需全局唯一，必填", example = "new_user", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度需为 4-32 位")
    private String username;

    /**
     * 昵称（缺省取用户名）
     */
    @Schema(description = "昵称，可选，最长 64 位，缺省取用户名", example = "新用户")
    @Size(max = 64, message = "昵称长度不能超过 64 位")
    private String nickname;

    /**
     * 密码（6-32 位）
     */
    @Schema(description = "登录密码，长度 6-32 位，必填", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
    private String password;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱，需符合邮箱格式，最长 128 位，需全局唯一，必填", example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 位")
    private String email;

    /**
     * 验证码标识（/auth/captcha 下发）
     */
    @Schema(description = "图形验证码标识，由 /auth/captcha 下发，有效期 5 分钟，必填", example = "3f8c1a9e5b7d4c2e",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    /**
     * 图形验证码答案
     */
    @Schema(description = "用户输入的图形验证码答案，不区分大小写，必填", example = "a1b2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}
