package com.github.wuzhudexiaopacai.oj.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色新增/编辑请求（管理端）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "角色新增/编辑请求（管理端）")
public class RoleSaveRequest implements Serializable {

    /**
     * 角色ID（编辑时必传，新增为空）
     */
    @Schema(description = "角色ID，编辑时必传，新增时为空", example = "1")
    private Long id;

    /**
     * 角色编码，如 ROLE_ADMIN（编辑时不可变更）
     */
    @Schema(description = "角色编码，最长 64 位，如 ROLE_ADMIN，需全局唯一，编辑时不可变更，必填", example = "ROLE_ADMIN",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 64, message = "角色编码长度不能超过 64 位")
    private String code;

    /**
     * 角色名称
     */
    @Schema(description = "角色名称，最长 64 位，必填", example = "管理员", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过 64 位")
    private String name;

    /**
     * 描述
     */
    @Schema(description = "角色描述，最长 255 位，可选", example = "拥有后台全部管理权限")
    @Size(max = 255, message = "描述长度不能超过 255 位")
    private String description;

    /**
     * 状态：1正常 0禁用
     */
    @Schema(description = "状态：1正常 0禁用，必填", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 权限ID列表（null 表示不调整权限绑定）
     */
    @Schema(description = "权限ID列表，null 表示不调整权限绑定，空列表表示清空绑定", example = "[1, 2, 3]")
    private List<Long> permissionIds;
}
