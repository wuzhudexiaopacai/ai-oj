package com.github.wuzhudexiaopacai.oj.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.github.wuzhudexiaopacai.oj.auth.constant.AuthConstants;
import com.github.wuzhudexiaopacai.oj.auth.dto.UserAdminSaveRequest;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysUser;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysPermissionMapper;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysRoleMapper;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysRolePermissionMapper;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysUserMapper;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysUserRoleMapper;
import com.github.wuzhudexiaopacai.oj.auth.service.TokenSessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户服务测试：禁用/删除账号时令牌失效
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysRolePermissionMapper rolePermissionMapper;

    @Mock
    private SysPermissionMapper permissionMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private TokenSessionService tokenSessionService;

    @InjectMocks
    private UserServiceImpl userService;

    private SysUser user(int status) {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("a@b.com");
        user.setStatus(status);
        return user;
    }

    @Test
    void updateUserShouldRevokeAllTokensWhenDisabled() {
        when(userMapper.selectById(1L)).thenReturn(user(AuthConstants.STATUS_ENABLE));
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        UserAdminSaveRequest request = new UserAdminSaveRequest();
        request.setId(1L);
        request.setEmail("a@b.com");
        request.setStatus(AuthConstants.STATUS_DISABLE);

        userService.updateUser(request);

        verify(tokenSessionService).revokeAll(1L);
    }

    @Test
    void updateUserShouldKeepTokensWhenEnabled() {
        when(userMapper.selectById(1L)).thenReturn(user(AuthConstants.STATUS_ENABLE));
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        UserAdminSaveRequest request = new UserAdminSaveRequest();
        request.setId(1L);
        request.setEmail("a@b.com");
        request.setStatus(AuthConstants.STATUS_ENABLE);

        userService.updateUser(request);

        verify(tokenSessionService, never()).revokeAll(1L);
    }

    @Test
    void deleteUserShouldRevokeAllTokens() {
        when(userMapper.selectById(1L)).thenReturn(user(AuthConstants.STATUS_ENABLE));

        userService.deleteUser(1L);

        verify(tokenSessionService).revokeAll(1L);
    }
}
