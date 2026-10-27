package com.github.wuzhudexiaopacai.oj.auth.controller.admin;

import com.github.wuzhudexiaopacai.oj.auth.dto.PermissionSaveRequest;
import com.github.wuzhudexiaopacai.oj.auth.service.PermissionService;
import com.github.wuzhudexiaopacai.oj.auth.vo.PermissionVO;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理接口（管理端，RBAC 鉴权）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "权限管理接口（管理端）", description = "管理端权限树查询与权限增删改；需登录并具备对应 permission:* 权限")
@RestController
@RequestMapping("/admin/permission")
@RequiredArgsConstructor
public class AdminPermissionController {

    private final PermissionService permissionService;

    /**
     * 权限树
     *
     * @return 树形权限列表
     */
    @Operation(summary = "权限树",
            description = "查询全部权限并按 parentId 组装为树形结构，同级按 sort、ID 升序；parentId 为空或父节点缺失的权限作为根节点展示。需 permission:list 权限。")
    @PreAuthorize("hasAuthority('permission:list')")
    @GetMapping("/tree")
    public Result<List<PermissionVO>> tree() {
        return Result.ok(permissionService.tree());
    }

    /**
     * 新增权限
     *
     * @param request 权限请求
     * @return 新权限ID
     */
    @Operation(summary = "新增权限",
            description = "需 permission:create 权限。权限标识必填且全局唯一，parentId 为 0 表示根节点，type 取值 1菜单 2按钮 3API，父权限需存在。返回新权限 ID。")
    @PreAuthorize("hasAuthority('permission:create')")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody PermissionSaveRequest request) {
        return Result.ok(permissionService.createPermission(request));
    }

    /**
     * 编辑权限
     *
     * @param request 权限请求
     * @return 空
     */
    @Operation(summary = "编辑权限",
            description = "需 permission:update 权限。id 必传，父权限不能设为自身，权限标识需保持全局唯一，type、path、method、sort、status 可调整。成功后清除绑定了该权限的角色下所有用户的信息缓存。")
    @PreAuthorize("hasAuthority('permission:update')")
    @PutMapping
    public Result<Void> update(@Validated @RequestBody PermissionSaveRequest request) {
        permissionService.updatePermission(request);
        return Result.ok();
    }

    /**
     * 删除权限
     *
     * @param id 权限ID
     * @return 空
     */
    @Operation(summary = "删除权限",
            description = "需 permission:delete 权限。存在子权限时拒绝删除；删除前清除受影响用户的信息缓存，并清理角色权限关联记录，删除后不可恢复。")
    @PreAuthorize("hasAuthority('permission:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "权限ID", required = true, example = "1") @PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.ok();
    }
}
