package com.github.wuzhudexiaopacai.oj.auth.controller.admin;

import com.github.wuzhudexiaopacai.oj.auth.dto.RolePageQueryRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.RoleSaveRequest;
import com.github.wuzhudexiaopacai.oj.auth.service.RoleService;
import com.github.wuzhudexiaopacai.oj.auth.vo.RoleVO;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
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
 * 角色管理接口（管理端，RBAC 鉴权）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@RestController
@RequestMapping("/admin/role")
@RequiredArgsConstructor
public class AdminRoleController {

    private final RoleService roleService;

    /**
     * 分页查询角色
     *
     * @param query 查询条件
     * @return 角色分页
     */
    @PreAuthorize("hasAuthority('role:list')")
    @GetMapping("/page")
    public Result<PageResult<RoleVO>> page(RolePageQueryRequest query) {
        return Result.ok(roleService.pageRoles(query));
    }

    /**
     * 全部启用角色（下拉选择场景）
     *
     * @return 角色列表
     */
    @PreAuthorize("hasAuthority('role:list')")
    @GetMapping("/all")
    public Result<List<RoleVO>> all() {
        return Result.ok(roleService.listAll());
    }

    /**
     * 角色详情（含权限绑定）
     *
     * @param id 角色ID
     * @return 角色信息
     */
    @PreAuthorize("hasAuthority('role:list')")
    @GetMapping("/{id}")
    public Result<RoleVO> detail(@PathVariable Long id) {
        return Result.ok(roleService.getRoleDetail(id));
    }

    /**
     * 新增角色
     *
     * @param request 角色请求
     * @return 新角色ID
     */
    @PreAuthorize("hasAuthority('role:create')")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody RoleSaveRequest request) {
        return Result.ok(roleService.createRole(request));
    }

    /**
     * 编辑角色（含权限绑定调整）
     *
     * @param request 角色请求
     * @return 空
     */
    @PreAuthorize("hasAuthority('role:update')")
    @PutMapping
    public Result<Void> update(@Validated @RequestBody RoleSaveRequest request) {
        roleService.updateRole(request);
        return Result.ok();
    }

    /**
     * 删除角色
     *
     * @param id 角色ID
     * @return 空
     */
    @PreAuthorize("hasAuthority('role:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.ok();
    }
}
