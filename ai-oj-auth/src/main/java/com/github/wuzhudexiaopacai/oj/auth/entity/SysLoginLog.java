package com.github.wuzhudexiaopacai.oj.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 登录日志实体（MQ 异步落库）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_login_log")
public class SysLoginLog extends BaseEntity {

    /**
     * 用户ID（登录失败可为空）
     */
    private Long userId;

    /**
     * 登录名
     */
    private String username;

    /**
     * 登录IP
     */
    private String ip;

    /**
     * UA
     */
    private String userAgent;

    /**
     * 是否成功：1成功 0失败
     */
    private Integer success;

    /**
     * 附加信息
     */
    private String message;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;
}
