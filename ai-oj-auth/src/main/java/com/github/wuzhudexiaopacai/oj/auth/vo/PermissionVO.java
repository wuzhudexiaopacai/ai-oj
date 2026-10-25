package com.github.wuzhudexiaopacai.oj.auth.vo;

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
public class PermissionVO implements Serializable {

    /**
     * 权限ID
     */
    private Long id;

    /**
     * 父权限ID，0为根
     */
    private Long parentId;

    /**
     * 权限标识
     */
    private String code;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 类型：1菜单 2按钮 3API
     */
    private Integer type;

    /**
     * 路由路径
     */
    private String path;

    /**
     * HTTP方法
     */
    private String method;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态：1正常 0禁用
     */
    private Integer status;

    /**
     * 子权限
     */
    private List<PermissionVO> children;
}
