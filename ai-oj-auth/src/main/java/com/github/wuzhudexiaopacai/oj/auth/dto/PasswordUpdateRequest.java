package com.github.wuzhudexiaopacai.oj.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 密码修改请求
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "密码修改请求")
public class PasswordUpdateRequest implements Serializable {

    /**
     * 原密码
     */
    @Schema(description = "原密码，必填，用于校验当前登录用户身份", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /**
     * 新密码（6-32 位）
     */
    @Schema(description = "新密码，长度 6-32 位，必填", example = "654321", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "新密码长度需为 6-32 位")
    private String newPassword;
}
