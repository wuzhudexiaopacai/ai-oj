package com.github.wuzhudexiaopacai.oj.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 个人资料修改请求
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "个人资料修改请求")
public class ProfileUpdateRequest implements Serializable {

    /**
     * 昵称
     */
    @Schema(description = "昵称，最长 64 位，必填", example = "小明", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过 64 位")
    private String nickname;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱，需符合邮箱格式，最长 128 位，不可与其他用户重复，必填", example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 位")
    private String email;

    /**
     * 头像URL
     */
    @Schema(description = "头像 URL，可选，最长 512 位，不传时保存为空字符串", example = "https://cdn.example.com/avatar.png")
    @Size(max = 512, message = "头像URL长度不能超过 512 位")
    private String avatar;

    /**
     * 个人简介
     */
    @Schema(description = "个人简介，可选，最长 512 位，不传时保存为空字符串", example = "热爱算法与竞赛")
    @Size(max = 512, message = "个人简介长度不能超过 512 位")
    private String intro;
}
