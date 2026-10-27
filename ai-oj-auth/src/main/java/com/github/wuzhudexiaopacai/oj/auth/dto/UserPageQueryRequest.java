package com.github.wuzhudexiaopacai.oj.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户分页查询请求（管理端）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "用户分页查询请求（管理端）")
public class UserPageQueryRequest implements Serializable {

    /**
     * 页码
     */
    @Schema(description = "页码，从 1 开始，默认 1", example = "1")
    private long pageNum = 1;

    /**
     * 每页大小
     */
    @Schema(description = "每页条数，默认 10", example = "10")
    private long pageSize = 10;

    /**
     * 登录名（模糊）
     */
    @Schema(description = "登录名，模糊匹配，可选", example = "admin")
    private String username;

    /**
     * 状态：1正常 0禁用
     */
    @Schema(description = "账号状态：1正常 0禁用，可选", example = "1")
    private Integer status;
}
