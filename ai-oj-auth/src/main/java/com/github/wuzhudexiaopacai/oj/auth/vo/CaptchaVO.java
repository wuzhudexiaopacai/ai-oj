package com.github.wuzhudexiaopacai.oj.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "图形验证码返回体")
public class CaptchaVO implements Serializable {

    /**
     * 验证码标识（登录/注册时回传）
     */
    @Schema(description = "验证码标识，登录/注册时需原样回传", example = "3f8c1a9e5b7d4c2e")
    private String captchaKey;

    /**
     * 验证码图片（data:image Base64）
     */
    @Schema(description = "验证码图片，data:image Base64 编码字符串", example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg...")
    private String img;
}
