package com.github.wuzhudexiaopacai.oj.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "用户新增/编辑请求（管理端）")
public class UserAdminSaveRequest implements Serializable {

    /**
     * 用户ID（编辑时必传，新增为空）
     */
    @Schema(description = "用户ID，编辑时必传，新增时为空", example = "1")
    private Long id;

    /**
     * 登录名（编辑时不可变更）
     */
    @Schema(description = "登录名，长度 4-32 位，需全局唯一，编辑时不可变更，必填", example = "alice",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度需为 4-32 位")
    private String username;

    /**
     * 昵称
     */
    @Schema(description = "昵称，可选，最长 64 位，缺省取用户名", example = "Alice")
    @Size(max = 64, message = "昵称长度不能超过 64 位")
    private String nickname;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱，需符合邮箱格式，最长 128 位，不可与其他用户重复，必填", example = "alice@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 位")
    private String email;

    /**
     * 初始/重置密码（新增必填；编辑留空表示不修改）
     */
    @Schema(description = "密码，长度 6-32 位；新增时必填，编辑时留空表示不修改", example = "123456")
    @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
    private String password;

    /**
     * 状态：1正常 0禁用
     */
    @Schema(description = "状态：1正常 0禁用，必填", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 角色ID列表（null 表示不调整角色）
     */
    @Schema(description = "角色ID列表，null 表示不调整角色，空列表表示清空角色", example = "[1, 2]")
    private List<Long> roleIds;
}
