package com.github.wuzhudexiaopacai.oj.auth.controller.admin;

import com.github.wuzhudexiaopacai.oj.auth.dto.PermissionSaveRequest;
import com.github.wuzhudexiaopacai.oj.auth.service.PermissionService;
import com.github.wuzhudexiaopacai.oj.auth.vo.PermissionVO;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
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
    @PreAuthorize("hasAuthority('permission:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.ok();
    }
}
