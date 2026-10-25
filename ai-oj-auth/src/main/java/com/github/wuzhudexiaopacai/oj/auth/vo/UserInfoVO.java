package com.github.wuzhudexiaopacai.oj.auth.vo;

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
public class UserInfoVO implements Serializable {

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
     * 角色编码集合
     */
    private List<String> roles;

    /**
     * 权限标识集合
     */
    private List<String> permissions;
}
