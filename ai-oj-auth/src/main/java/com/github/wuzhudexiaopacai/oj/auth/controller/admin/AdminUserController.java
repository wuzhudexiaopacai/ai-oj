package com.github.wuzhudexiaopacai.oj.auth.controller.admin;

import com.github.wuzhudexiaopacai.oj.auth.dto.AssignRolesRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.UserAdminSaveRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.UserPageQueryRequest;
import com.github.wuzhudexiaopacai.oj.auth.service.UserService;
import com.github.wuzhudexiaopacai.oj.auth.vo.UserAdminVO;
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

/**
 * 用户管理接口（管理端，RBAC 鉴权）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "用户管理接口（管理端）", description = "管理端用户分页、详情、增删改与角色分配；需登录并具备对应 user:* 权限")
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    /**
     * 分页查询用户
     *
     * @param query 查询条件
     * @return 用户分页
     */
    @Operation(summary = "分页查询用户",
            description = "按登录名模糊匹配、按状态精确筛选，结果按创建时间倒序；页码从 1 开始，每页条数默认 10。需 user:list 权限，返回用户分页并填充各用户的角色 ID 列表。")
    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping("/page")
    public Result<PageResult<UserAdminVO>> page(@ParameterObject UserPageQueryRequest query) {
        return Result.ok(userService.pageUsers(query));
    }

    /**
     * 用户详情
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Operation(summary = "用户详情",
            description = "按用户 ID 查询管理端用户视图，包含状态、做题统计、Rating、完整角色 ID 列表及创建/更新时间；用户不存在时返回业务异常。需 user:list 权限。")
    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping("/{id}")
    public Result<UserAdminVO> detail(@Parameter(description = "用户ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(userService.getUserDetail(id));
    }

    /**
     * 新增用户
     *
     * @param request 用户请求
     * @return 新用户ID
     */
    @Operation(summary = "新增用户",
            description = "需 user:create 权限。初始密码必填，用户名与邮箱需全局唯一，昵称缺省取用户名，Rating 初始 1500；roleIds 非 null 时全量绑定角色。返回新用户 ID。")
    @PreAuthorize("hasAuthority('user:create')")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody UserAdminSaveRequest request) {
        return Result.ok(userService.createUser(request));
    }

    /**
     * 编辑用户
     *
     * @param request 用户请求
     * @return 空
     */
    @Operation(summary = "编辑用户",
            description = "需 user:update 权限。id 必传，用户名不可变更，邮箱需与他人的不重复，password 留空表示不修改密码，status 必传；roleIds 非 null 时全量替换角色。成功后清除该用户信息缓存。")
    @PreAuthorize("hasAuthority('user:update')")
    @PutMapping
    public Result<Void> update(@Validated @RequestBody UserAdminSaveRequest request) {
        userService.updateUser(request);
        return Result.ok();
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 空
     */
    @Operation(summary = "删除用户",
            description = "需 user:delete 权限。删除用户及其角色关联记录，并清除该用户信息缓存；删除后不可恢复。用户不存在时返回业务异常。")
    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "用户ID", required = true, example = "1") @PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }

    /**
     * 分配用户角色
     *
     * @param request 分配请求
     * @return 空
     */
    @Operation(summary = "分配用户角色",
            description = "需 user:assign-role 权限。全量替换目标用户的角色绑定，空列表表示清空全部角色，角色 ID 必须均有效。成功后清除该用户信息缓存。")
    @PreAuthorize("hasAuthority('user:assign-role')")
    @PutMapping("/roles")
    public Result<Void> assignRoles(@Validated @RequestBody AssignRolesRequest request) {
        userService.assignRoles(request);
        return Result.ok();
    }
}
