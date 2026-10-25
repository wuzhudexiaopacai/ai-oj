package com.github.wuzhudexiaopacai.oj.auth.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色信息（管理端视图）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class RoleVO implements Serializable {

    /**
     * 角色ID
     */
    private Long id;

    /**
     * 角色编码
     */
    private String code;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态：1正常 0禁用
     */
    private Integer status;

    /**
     * 已绑定权限ID列表（详情时填充）
     */
    private List<Long> permissionIds;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
