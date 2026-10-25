package com.github.wuzhudexiaopacai.oj.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 图形验证码返回体
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@AllArgsConstructor
public class CaptchaVO implements Serializable {

    /**
     * 验证码标识（登录/注册时回传）
     */
    private String captchaKey;

    /**
     * 验证码图片（data:image Base64）
     */
    private String img;
}
