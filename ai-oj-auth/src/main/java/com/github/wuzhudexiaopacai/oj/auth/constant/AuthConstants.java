package com.github.wuzhudexiaopacai.oj.auth.constant;

/**
 * 认证中心常量
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public class AuthConstants {

    private AuthConstants() {
    }

    /**
     * 注册默认角色编码
     */
    public static final String DEFAULT_ROLE_CODE = "ROLE_USER";

    /**
     * 状态：正常
     */
    public static final int STATUS_ENABLE = 1;

    /**
     * 状态：禁用
     */
    public static final int STATUS_DISABLE = 0;

    /**
     * 图形验证码有效期（分钟）
     */
    public static final long CAPTCHA_EXPIRE_MINUTES = 5L;

    /**
     * 用户信息缓存有效期（分钟）
     */
    public static final long USER_INFO_CACHE_MINUTES = 30L;
}
