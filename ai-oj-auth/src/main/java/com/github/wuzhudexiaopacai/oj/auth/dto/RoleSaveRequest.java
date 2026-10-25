package com.github.wuzhudexiaopacai.oj.auth.dto;

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
public class RoleSaveRequest implements Serializable {

    /**
     * 角色ID（编辑时必传，新增为空）
     */
    private Long id;

    /**
     * 角色编码，如 ROLE_ADMIN（编辑时不可变更）
     */
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 64, message = "角色编码长度不能超过 64 位")
    private String code;

    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过 64 位")
    private String name;

    /**
     * 描述
     */
    @Size(max = 255, message = "描述长度不能超过 255 位")
    private String description;

    /**
     * 状态：1正常 0禁用
     */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 权限ID列表（null 表示不调整权限绑定）
     */
    private List<Long> permissionIds;
}
