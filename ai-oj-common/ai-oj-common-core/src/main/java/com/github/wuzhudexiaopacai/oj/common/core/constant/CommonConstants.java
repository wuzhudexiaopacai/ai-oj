package com.github.wuzhudexiaopacai.oj.common.core.constant;

/**
 * 通用常量
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public class CommonConstants {

    private CommonConstants() {
    }

    /**
     * 网关注入的登录用户 ID 请求头
     */
    public static final String HEADER_USER_ID = "X-User-Id";

    /**
     * 网关注入的登录用户名请求头
     */
    public static final String HEADER_USER_NAME = "X-User-Name";

    /**
     * 网关注入的权限标识请求头（逗号分隔）
     */
    public static final String HEADER_AUTHORITIES = "X-User-Authorities";

    /**
     * 内部服务调用标识头（仅内网/Feign 使用，网关会剥离外部传入的该头）
     */
    public static final String HEADER_INTERNAL_CALL = "X-Internal-Call";

    /**
     * 链路追踪 ID 请求头（网关生成，业务服务透传，日志表与运行日志共用）
     */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /**
     * Token 请求头
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * Token 前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * JWT claim：用户 ID
     */
    public static final String JWT_CLAIM_UID = "uid";

    /**
     * JWT claim：权限标识
     */
    public static final String JWT_CLAIM_AUTHORITIES = "auths";

    /**
     * JWT claim：昵称
     */
    public static final String JWT_CLAIM_NICKNAME = "nickname";
}
