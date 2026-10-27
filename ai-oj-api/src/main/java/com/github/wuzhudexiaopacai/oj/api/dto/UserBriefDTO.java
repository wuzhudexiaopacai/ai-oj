package com.github.wuzhudexiaopacai.oj.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户简要信息（跨服务传输）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "用户简要信息（内部 Feign 契约，供比赛排行榜等场景回填用户展示信息）")
public class UserBriefDTO implements Serializable {

    @Schema(description = "用户 ID（雪花 ID）", example = "1")
    private Long id;

    @Schema(description = "登录名", example = "alice")
    private String username;

    @Schema(description = "昵称，为空时前端展示用户名", example = "Alice")
    private String nickname;

    @Schema(description = "头像访问地址，可为空", example = "https://example.com/avatar.png")
    private String avatar;

    @Schema(description = "个人简介，可为空", example = "热爱算法")
    private String intro;
}
