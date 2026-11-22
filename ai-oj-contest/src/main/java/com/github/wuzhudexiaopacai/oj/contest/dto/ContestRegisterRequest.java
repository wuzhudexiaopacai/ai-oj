package com.github.wuzhudexiaopacai.oj.contest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 比赛报名请求
 * <p>
 * 密码放在请求体而非查询参数，避免进入访问日志、浏览器历史与代理日志。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "比赛报名请求（私有赛需在请求体携带密码）")
public class ContestRegisterRequest {

    @Size(max = 64, message = "比赛密码长度不能超过 64")
    @Schema(description = "私有赛密码；公开赛可省略", example = "123456")
    private String password;
}
