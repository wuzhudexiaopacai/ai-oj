package com.github.wuzhudexiaopacai.oj.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "权限新增/编辑请求（管理端）")
public class PermissionSaveRequest implements Serializable {

    /**
     * 权限ID（编辑时必传，新增为空）
     */
    @Schema(description = "权限ID，编辑时必传，新增时为空", example = "1")
    private Long id;

    /**
     * 父权限ID，0为根
     */
    @Schema(description = "父权限ID，0 表示根节点，父权限需存在，必填", example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "父权限ID不能为空")
    private Long parentId;

    /**
     * 权限标识，如 problem:create
     */
    @Schema(description = "权限标识，最长 128 位，如 problem:create，需全局唯一，必填", example = "problem:create",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "权限标识不能为空")
    @Size(max = 128, message = "权限标识长度不能超过 128 位")
    private String code;

    /**
     * 权限名称
     */
    @Schema(description = "权限名称，最长 64 位，必填", example = "新增题目", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "权限名称不能为空")
    @Size(max = 64, message = "权限名称长度不能超过 64 位")
    private String name;

    /**
     * 类型：1菜单 2按钮 3API
     */
    @Schema(description = "权限类型：1菜单 2按钮 3API，取值 1-3，必填", example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "类型不能为空")
    @Min(value = 1, message = "类型取值 1-3")
    @Max(value = 3, message = "类型取值 1-3")
    private Integer type;

    /**
     * 路由路径
     */
    @Schema(description = "路由路径，最长 255 位，可选", example = "/admin/problem")
    @Size(max = 255, message = "路由路径长度不能超过 255 位")
    private String path;

    /**
     * HTTP方法（API类型用）
     */
    @Schema(description = "HTTP 方法，最长 8 位，仅 API 类型使用，可选", example = "POST")
    @Size(max = 8, message = "HTTP方法长度不能超过 8 位")
    private String method;

    /**
     * 排序
     */
    @Schema(description = "排序值，数值越小越靠前，不传默认为 0", example = "1")
    private Integer sort;

    /**
     * 状态：1正常 0禁用
     */
    @Schema(description = "状态：1正常 0禁用，必填", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;
}
