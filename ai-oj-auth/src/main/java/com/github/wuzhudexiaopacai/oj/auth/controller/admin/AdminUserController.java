package com.github.wuzhudexiaopacai.oj.auth.controller.admin;

import com.github.wuzhudexiaopacai.oj.auth.dto.AssignRolesRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.UserAdminSaveRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.UserPageQueryRequest;
import com.github.wuzhudexiaopacai.oj.auth.service.UserService;
import com.github.wuzhudexiaopacai.oj.auth.vo.UserAdminVO;
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

/**
 * 用户管理接口（管理端，RBAC 鉴权）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
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
    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping("/page")
    public Result<PageResult<UserAdminVO>> page(UserPageQueryRequest query) {
        return Result.ok(userService.pageUsers(query));
    }

    /**
     * 用户详情
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping("/{id}")
    public Result<UserAdminVO> detail(@PathVariable Long id) {
        return Result.ok(userService.getUserDetail(id));
    }

    /**
     * 新增用户
     *
     * @param request 用户请求
     * @return 新用户ID
     */
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
    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }

    /**
     * 分配用户角色
     *
     * @param request 分配请求
     * @return 空
     */
    @PreAuthorize("hasAuthority('user:assign-role')")
    @PutMapping("/roles")
    public Result<Void> assignRoles(@Validated @RequestBody AssignRolesRequest request) {
        userService.assignRoles(request);
        return Result.ok();
    }
}
