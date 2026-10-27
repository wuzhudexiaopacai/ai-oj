package com.github.wuzhudexiaopacai.oj.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 权限信息（管理端视图，树形）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "权限信息（管理端视图，树形）")
public class PermissionVO implements Serializable {

    /**
     * 权限ID
     */
    @Schema(description = "权限ID", example = "1")
    private Long id;

    /**
     * 父权限ID，0为根
     */
    @Schema(description = "父权限ID，0 表示根节点", example = "0")
    private Long parentId;

    /**
     * 权限标识
     */
    @Schema(description = "权限标识", example = "problem:create")
    private String code;

    /**
     * 权限名称
     */
    @Schema(description = "权限名称", example = "新增题目")
    private String name;

    /**
     * 类型：1菜单 2按钮 3API
     */
    @Schema(description = "权限类型：1菜单 2按钮 3API", example = "2")
    private Integer type;

    /**
     * 路由路径
     */
    @Schema(description = "路由路径", example = "/admin/problem")
    private String path;

    /**
     * HTTP方法
     */
    @Schema(description = "HTTP 方法，仅 API 类型使用", example = "POST")
    private String method;

    /**
     * 排序
     */
    @Schema(description = "排序值，数值越小越靠前", example = "1")
    private Integer sort;

    /**
     * 状态：1正常 0禁用
     */
    @Schema(description = "状态：1正常 0禁用", example = "1")
    private Integer status;

    /**
     * 子权限
     */
    @Schema(description = "子权限列表，按树形结构嵌套")
    private List<PermissionVO> children;
}
