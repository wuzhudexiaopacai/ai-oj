package com.github.wuzhudexiaopacai.oj.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "角色信息（管理端视图）")
public class RoleVO implements Serializable {

    /**
     * 角色ID
     */
    @Schema(description = "角色ID", example = "1")
    private Long id;

    /**
     * 角色编码
     */
    @Schema(description = "角色编码", example = "ROLE_ADMIN")
    private String code;

    /**
     * 角色名称
     */
    @Schema(description = "角色名称", example = "管理员")
    private String name;

    /**
     * 描述
     */
    @Schema(description = "角色描述", example = "拥有后台全部管理权限")
    private String description;

    /**
     * 状态：1正常 0禁用
     */
    @Schema(description = "角色状态：1正常 0禁用", example = "1")
    private Integer status;

    /**
     * 已绑定权限ID列表（详情时填充）
     */
    @Schema(description = "已绑定权限ID列表，仅角色详情查询时填充", example = "[1, 2, 3]")
    private List<Long> permissionIds;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间，格式 yyyy-MM-dd HH:mm:ss", example = "2026-01-01 12:00:00")
    private LocalDateTime createTime;
}
