package com.github.wuzhudexiaopacai.oj.common.web.util;

import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.core.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 登录态工具
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户（未登录返回 null）
     *
     * @return 登录用户
     */
    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    /**
     * 获取当前登录用户 ID（未登录返回 null）
     *
     * @return 用户 ID
     */
    public static Long currentUserIdOrNull() {
        LoginUser loginUser = currentUser();
        return loginUser == null ? null : loginUser.getId();
    }

    /**
     * 要求必须登录，否则抛 401
     *
     * @return 登录用户
     */
    public static LoginUser requireLogin() {
        LoginUser loginUser = currentUser();
        if (loginUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }
}
