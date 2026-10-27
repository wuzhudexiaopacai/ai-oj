package com.github.wuzhudexiaopacai.oj.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息（管理端视图）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "用户信息（管理端视图）")
public class UserAdminVO implements Serializable {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1")
    private Long id;

    /**
     * 登录名
     */
    @Schema(description = "登录名", example = "admin")
    private String username;

    /**
     * 昵称
     */
    @Schema(description = "昵称", example = "管理员")
    private String nickname;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;

    /**
     * 头像URL
     */
    @Schema(description = "头像 URL", example = "https://cdn.example.com/avatar.png")
    private String avatar;

    /**
     * 个人简介
     */
    @Schema(description = "个人简介", example = "热爱算法与竞赛")
    private String intro;

    /**
     * 状态：1正常 0禁用
     */
    @Schema(description = "账号状态：1正常 0禁用", example = "1")
    private Integer status;

    /**
     * 通过题数
     */
    @Schema(description = "通过题数", example = "128")
    private Integer acceptCount;

    /**
     * 提交总数
     */
    @Schema(description = "提交总数", example = "256")
    private Integer submitCount;

    /**
     * 竞赛 Rating
     */
    @Schema(description = "竞赛 Rating，注册初始值为 1500", example = "1500")
    private Integer rating;

    /**
     * 角色ID列表（详情/分页时填充）
     */
    @Schema(description = "角色ID列表，分页与详情查询时填充", example = "[1, 2]")
    private List<Long> roleIds;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间，格式 yyyy-MM-dd HH:mm:ss", example = "2026-01-01 12:00:00")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间，格式 yyyy-MM-dd HH:mm:ss", example = "2026-01-02 08:30:00")
    private LocalDateTime updateTime;
}
