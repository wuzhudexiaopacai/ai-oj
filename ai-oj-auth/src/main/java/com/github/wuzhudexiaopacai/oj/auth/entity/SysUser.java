package com.github.wuzhudexiaopacai.oj.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /**
     * 登录名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * BCrypt 密码哈希
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 状态：1正常 0禁用
     */
    private Integer status;

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
}
