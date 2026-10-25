package com.github.wuzhudexiaopacai.oj.auth.controller;

import com.github.wuzhudexiaopacai.oj.auth.dto.PasswordUpdateRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.ProfileUpdateRequest;
import com.github.wuzhudexiaopacai.oj.auth.service.UserService;
import com.github.wuzhudexiaopacai.oj.auth.vo.UserInfoVO;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
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
    @PutMapping("/password")
    public Result<Void> updatePassword(@Validated @RequestBody PasswordUpdateRequest request) {
        userService.updatePassword(request);
        return Result.ok();
    }
}
