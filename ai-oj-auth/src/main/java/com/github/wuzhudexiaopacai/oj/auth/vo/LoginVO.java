package com.github.wuzhudexiaopacai.oj.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录返回体
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@AllArgsConstructor
@Schema(description = "登录返回体")
public class LoginVO implements Serializable {

    /**
     * JWT 令牌（请求头携带：Authorization: Bearer {token}）
     */
    @Schema(description = "JWT 令牌，后续请求需放入请求头 Authorization: Bearer {token}",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature")
    private String token;

    /**
     * 登录用户信息
     */
    @Schema(description = "登录用户信息，含角色编码与权限标识集合")
    private UserInfoVO userInfo;
}
