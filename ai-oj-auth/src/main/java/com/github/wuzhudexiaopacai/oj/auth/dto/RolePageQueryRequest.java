package com.github.wuzhudexiaopacai.oj.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 角色分页查询请求（管理端）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class RolePageQueryRequest implements Serializable {

    /**
     * 页码
     */
    private long pageNum = 1;

    /**
     * 每页大小
     */
    private long pageSize = 10;

    /**
     * 关键字（编码/名称模糊）
     */
    private String keyword;

    /**
     * 状态：1正常 0禁用
     */
    private Integer status;
}
