package com.github.wuzhudexiaopacai.oj.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "用户角色分配请求（管理端）")
public class AssignRolesRequest implements Serializable {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID，必填", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 角色ID列表（空列表表示清空角色）
     */
    @Schema(description = "角色ID列表，必填；空列表表示清空该用户全部角色", example = "[1, 2]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "角色列表不能为空")
    private List<Long> roleIds;
}
