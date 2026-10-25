package com.github.wuzhudexiaopacai.oj.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户角色分配请求（管理端）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class AssignRolesRequest implements Serializable {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 角色ID列表（空列表表示清空角色）
     */
    @NotNull(message = "角色列表不能为空")
    private List<Long> roleIds;
}
