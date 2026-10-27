package com.github.wuzhudexiaopacai.oj.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "登录请求")
public class LoginRequest implements Serializable {

    /**
     * 登录名
     */
    @Schema(description = "登录名，必填", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @Schema(description = "登录密码，必填", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    private String password;

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
