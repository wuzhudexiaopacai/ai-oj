package com.github.wuzhudexiaopacai.oj.auth.vo;

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
public class LoginVO implements Serializable {

    /**
     * JWT 令牌（请求头携带：Authorization: Bearer {token}）
     */
    private String token;

    /**
     * 登录用户信息
     */
    private UserInfoVO userInfo;
}
