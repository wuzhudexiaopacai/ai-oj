package com.github.wuzhudexiaopacai.oj.auth.controller.admin;

import com.github.wuzhudexiaopacai.oj.auth.dto.RolePageQueryRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.RoleSaveRequest;
import com.github.wuzhudexiaopacai.oj.auth.service.RoleService;
import com.github.wuzhudexiaopacai.oj.auth.vo.RoleVO;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "角色管理接口（管理端）", description = "管理端角色分页、详情、增删改与权限绑定；需登录并具备对应 role:* 权限")
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
    @Operation(summary = "分页查询角色",
            description = "关键字同时模糊匹配角色编码与名称，状态精确筛选，结果按角色 ID 升序；页码从 1 开始，每页条数默认 10。需 role:list 权限。")
    @PreAuthorize("hasAuthority('role:list')")
    @GetMapping("/page")
    public Result<PageResult<RoleVO>> page(@ParameterObject RolePageQueryRequest query) {
        return Result.ok(roleService.pageRoles(query));
    }

    /**
     * 全部启用角色（下拉选择场景）
     *
     * @return 角色列表
     */
    @Operation(summary = "全部启用角色",
            description = "返回状态为 1（正常）的全部角色，按角色 ID 升序，用于下拉选择等场景，不包含已绑定权限明细。需 role:list 权限。")
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
    @Operation(summary = "角色详情",
            description = "按角色 ID 查询角色信息，并填充已绑定的权限 ID 列表；角色不存在时返回业务异常。需 role:list 权限。")
    @PreAuthorize("hasAuthority('role:list')")
    @GetMapping("/{id}")
    public Result<RoleVO> detail(@Parameter(description = "角色ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(roleService.getRoleDetail(id));
    }

    /**
     * 新增角色
     *
     * @param request 角色请求
     * @return 新角色ID
     */
    @Operation(summary = "新增角色",
            description = "需 role:create 权限。角色编码必填且全局唯一；permissionIds 非 null 时全量绑定权限，其中的权限 ID 必须均有效。返回新角色 ID。")
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
    @Operation(summary = "编辑角色",
            description = "需 role:update 权限。id 必传，角色编码作为权限上下文标识不可变更，可调整名称、描述、状态与权限绑定（permissionIds 为 null 表示不调整）。成功后清除该角色下所有用户的信息缓存。")
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
    @Operation(summary = "删除角色",
            description = "需 role:delete 权限。先清除该角色下用户的信息缓存，再删除角色及其权限绑定、用户角色关联记录，删除后不可恢复。角色不存在时返回业务异常。")
    @PreAuthorize("hasAuthority('role:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "角色ID", required = true, example = "1") @PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.ok();
    }
}
