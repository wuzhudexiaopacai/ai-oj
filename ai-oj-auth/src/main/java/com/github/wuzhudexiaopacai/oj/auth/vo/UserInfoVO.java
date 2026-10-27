package com.github.wuzhudexiaopacai.oj.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录用户信息
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "登录用户信息")
public class UserInfoVO implements Serializable {

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
     * 角色编码集合
     */
    @Schema(description = "角色编码集合，仅包含启用状态的角色", example = "[\"ROLE_USER\"]")
    private List<String> roles;

    /**
     * 权限标识集合
     */
    @Schema(description = "权限标识集合，仅包含启用状态权限", example = "[\"problem:list\", \"submission:submit\"]")
    private List<String> permissions;
}
