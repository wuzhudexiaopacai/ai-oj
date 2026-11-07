package com.github.wuzhudexiaopacai.oj.auth.controller;

import com.github.wuzhudexiaopacai.oj.auth.dto.PasswordUpdateRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.ProfileUpdateRequest;
import com.github.wuzhudexiaopacai.oj.auth.service.UserService;
import com.github.wuzhudexiaopacai.oj.auth.vo.UserInfoVO;
import com.github.wuzhudexiaopacai.oj.common.core.enums.OpType;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录用户接口
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "当前用户接口", description = "当前登录用户的资料查询、修改与密码变更；全部接口均需登录")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    @Operation(summary = "获取当前登录用户信息",
            description = "从登录上下文取当前用户 ID，优先读取 Redis 用户信息缓存（30 分钟），未命中时回源数据库并按启用角色与权限重新构建。返回昵称、邮箱、头像、简介、做题统计、Rating 及角色/权限集合。需要登录。")
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.ok(userService.getCurrentUserInfo());
    }

    /**
     * 修改个人资料
     *
     * @param request 资料请求
     * @return 空
     */
    @Operation(summary = "修改个人资料",
            description = "仅修改当前登录用户的昵称、邮箱、头像与个人简介，邮箱不可与其他用户重复；未传的头像/简介会置为空字符串。修改成功后立即清除该用户信息缓存。需要登录。")
    @OperationLog(module = "个人中心", operation = "修改个人资料", type = OpType.UPDATE)
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Validated @RequestBody ProfileUpdateRequest request) {
        userService.updateProfile(request);
        return Result.ok();
    }

    /**
     * 修改密码
     *
     * @param request 密码请求
     * @return 空
     */
    @Operation(summary = "修改密码",
            description = "校验原密码正确后更新为新密码（6-32 位），并清除用户信息缓存。已签发的令牌在到期前仍然有效，如需强制下线需另行调用登出接口。需要登录。")
    @OperationLog(module = "个人中心", operation = "修改密码", type = OpType.UPDATE, recordResult = false)
    @PutMapping("/password")
    public Result<Void> updatePassword(@Validated @RequestBody PasswordUpdateRequest request) {
        userService.updatePassword(request);
        return Result.ok();
    }
}
