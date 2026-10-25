package com.github.wuzhudexiaopacai.oj.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 权限新增/编辑请求（管理端）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class PermissionSaveRequest implements Serializable {

    /**
     * 权限ID（编辑时必传，新增为空）
     */
    private Long id;

    /**
     * 父权限ID，0为根
     */
    @NotNull(message = "父权限ID不能为空")
    private Long parentId;

    /**
     * 权限标识，如 problem:create
     */
    @NotBlank(message = "权限标识不能为空")
    @Size(max = 128, message = "权限标识长度不能超过 128 位")
    private String code;

    /**
     * 权限名称
     */
    @NotBlank(message = "权限名称不能为空")
    @Size(max = 64, message = "权限名称长度不能超过 64 位")
    private String name;

    /**
     * 类型：1菜单 2按钮 3API
     */
    @NotNull(message = "类型不能为空")
    @Min(value = 1, message = "类型取值 1-3")
    @Max(value = 3, message = "类型取值 1-3")
    private Integer type;

    /**
     * 路由路径
     */
    @Size(max = 255, message = "路由路径长度不能超过 255 位")
    private String path;

    /**
     * HTTP方法（API类型用）
     */
    @Size(max = 8, message = "HTTP方法长度不能超过 8 位")
    private String method;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态：1正常 0禁用
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
