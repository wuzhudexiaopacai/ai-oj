package com.github.wuzhudexiaopacai.oj.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户新增/编辑请求（管理端）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class UserAdminSaveRequest implements Serializable {

    /**
     * 用户ID（编辑时必传，新增为空）
     */
    private Long id;

    /**
     * 登录名（编辑时不可变更）
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度需为 4-32 位")
    private String username;

    /**
     * 昵称
     */
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
     * 初始/重置密码（新增必填；编辑留空表示不修改）
     */
    @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
    private String password;

    /**
     * 状态：1正常 0禁用
     */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 角色ID列表（null 表示不调整角色）
     */
    private List<Long> roleIds;
}
