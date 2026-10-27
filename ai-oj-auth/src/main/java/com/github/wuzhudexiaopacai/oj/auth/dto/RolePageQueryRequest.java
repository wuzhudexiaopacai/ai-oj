package com.github.wuzhudexiaopacai.oj.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色分页查询请求（管理端）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "角色分页查询请求（管理端）")
public class RolePageQueryRequest implements Serializable {

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
     * 关键字（编码/名称模糊）
     */
    @Schema(description = "关键字，按角色编码或名称模糊匹配，可选", example = "ROLE_")
    private String keyword;

    /**
     * 状态：1正常 0禁用
     */
    @Schema(description = "角色状态：1正常 0禁用，可选", example = "1")
    private Integer status;
}
