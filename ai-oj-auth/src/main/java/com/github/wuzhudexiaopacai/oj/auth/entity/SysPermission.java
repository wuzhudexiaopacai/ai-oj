package com.github.wuzhudexiaopacai.oj.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限实体（菜单/按钮/API 三级）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    /**
     * 父权限ID，0为根
     */
    private Long parentId;

    /**
     * 权限标识，如 problem:create
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
     * HTTP方法（API类型用）
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
}
