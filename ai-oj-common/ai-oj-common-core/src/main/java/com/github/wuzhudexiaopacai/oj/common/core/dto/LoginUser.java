package com.github.wuzhudexiaopacai.oj.common.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 登录用户上下文（网关解析 JWT 后通过请求头透传给各服务）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 权限标识集合（RBAC，如 problem:create）
     */
    private List<String> authorities;

    /**
     * Token 唯一标识（jti，用于注销黑名单）
     */
    private String tokenId;

    /**
     * 是否拥有指定权限
     *
     * @param authority 权限标识
     * @return true 拥有
     */
    public boolean hasAuthority(String authority) {
        return authorities != null && authorities.contains(authority);
    }
}
