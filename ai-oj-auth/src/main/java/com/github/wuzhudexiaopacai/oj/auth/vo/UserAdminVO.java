package com.github.wuzhudexiaopacai.oj.auth.vo;

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
public class UserAdminVO implements Serializable {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 登录名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 个人简介
     */
    private String intro;

    /**
     * 状态：1正常 0禁用
     */
    private Integer status;

    /**
     * 通过题数
     */
    private Integer acceptCount;

    /**
     * 提交总数
     */
    private Integer submitCount;

    /**
     * 竞赛 Rating
     */
    private Integer rating;

    /**
     * 角色ID列表（详情/分页时填充）
     */
    private List<Long> roleIds;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
