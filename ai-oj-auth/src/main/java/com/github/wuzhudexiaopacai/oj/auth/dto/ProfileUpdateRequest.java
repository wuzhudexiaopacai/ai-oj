package com.github.wuzhudexiaopacai.oj.auth.dto;

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
public class ProfileUpdateRequest implements Serializable {

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过 64 位")
    private String nickname;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 位")
    private String email;

    /**
     * 头像URL
     */
    @Size(max = 512, message = "头像URL长度不能超过 512 位")
    private String avatar;

    /**
     * 个人简介
     */
    @Size(max = 512, message = "个人简介长度不能超过 512 位")
    private String intro;
}
