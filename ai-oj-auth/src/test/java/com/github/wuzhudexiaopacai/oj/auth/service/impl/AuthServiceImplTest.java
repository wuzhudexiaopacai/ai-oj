package com.github.wuzhudexiaopacai.oj.auth.service.impl;

import com.github.wuzhudexiaopacai.oj.auth.constant.AuthConstants;
import com.github.wuzhudexiaopacai.oj.auth.dto.LoginRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.RegisterRequest;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysUser;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysRoleMapper;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysUserMapper;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysUserRoleMapper;
import com.github.wuzhudexiaopacai.oj.auth.mq.LoginLogProducer;
import com.github.wuzhudexiaopacai.oj.auth.service.TokenSessionService;
import com.github.wuzhudexiaopacai.oj.auth.service.UserService;
import com.github.wuzhudexiaopacai.oj.auth.vo.LoginVO;
import com.github.wuzhudexiaopacai.oj.auth.vo.UserInfoVO;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.core.util.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证服务测试：登录/刷新/登出的令牌索引维护
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserService userService;

    @Mock
    private LoginLogProducer loginLogProducer;

    @Mock
    private TokenSessionService tokenSessionService;

    @InjectMocks
    private AuthServiceImpl authService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "loginMaxFailures", 5);
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "loginLockMinutes", 5L);
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "registerIpLimit", 5);
    }

    private SysUser user() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("alice");
        user.setNickname("Alice");
        user.setPassword("hash");
        user.setStatus(AuthConstants.STATUS_ENABLE);
        return user;
    }

    private UserInfoVO userInfo() {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(1L);
        vo.setUsername("alice");
        vo.setRoles(List.of("USER"));
        vo.setPermissions(List.of());
        return vo;
    }

    @Test
    void loginShouldRegisterIssuedToken() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.LOGIN_FAIL + "alice")).thenReturn(null);
        when(valueOperations.get(RedisKeys.CAPTCHA + "ck")).thenReturn("abcd");
        when(userMapper.selectOne(any())).thenReturn(user());
        when(passwordEncoder.matches("pwd", "hash")).thenReturn(true);
        when(userService.buildUserInfo(any(SysUser.class))).thenReturn(userInfo());
        when(jwtUtils.generateToken(any(LoginUser.class))).thenAnswer(invocation -> {
            LoginUser loginUser = invocation.getArgument(0);
            loginUser.setTokenId("jti-new");
            return "token";
        });

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("pwd");
        request.setCaptchaKey("ck");
        request.setCaptchaCode("abcd");
        LoginVO result = authService.login(request, "127.0.0.1", "ua");

        assertEquals("token", result.getToken());
        verify(tokenSessionService).register(1L, "jti-new");
        verify(stringRedisTemplate).delete(RedisKeys.LOGIN_FAIL + "alice");
    }

    @Test
    void loginShouldRejectWhenFailuresReachLimit() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.LOGIN_FAIL + "alice")).thenReturn("5");

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("pwd");

        assertThrows(BizException.class,
                () -> authService.login(request, "127.0.0.1", "ua"));
        // 达到锁定阈值后不再校验验证码与密码
        verify(userMapper, never()).selectOne(any());
        verify(loginLogProducer).send(null, "alice", "127.0.0.1", "ua", 0, "登录失败次数过多，已临时锁定");
    }

    @Test
    void loginShouldRecordFailureAndSetExpireOnFirstFailure() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.LOGIN_FAIL + "alice")).thenReturn(null);
        when(valueOperations.get(RedisKeys.CAPTCHA + "ck")).thenReturn("abcd");
        when(userMapper.selectOne(any())).thenReturn(user());
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);
        when(valueOperations.increment(RedisKeys.LOGIN_FAIL + "alice")).thenReturn(1L);

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("bad");
        request.setCaptchaKey("ck");
        request.setCaptchaCode("abcd");

        assertThrows(BizException.class,
                () -> authService.login(request, "127.0.0.1", "ua"));
        verify(stringRedisTemplate).expire(RedisKeys.LOGIN_FAIL + "alice", 5L, TimeUnit.MINUTES);
    }

    @Test
    void registerShouldRejectWhenIpLimitReached() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.CAPTCHA + "ck")).thenReturn("abcd");
        when(valueOperations.increment(RedisKeys.REGISTER_IP + "1.2.3.4")).thenReturn(6L);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("bob");
        request.setPassword("pwd");
        request.setEmail("b@b.com");
        request.setCaptchaKey("ck");
        request.setCaptchaCode("abcd");

        assertThrows(BizException.class,
                () -> authService.register(request, "1.2.3.4", "ua"));
        verify(userMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void refreshShouldRevokeOldTokenAndRegisterNewOne() {
        LoginUser oldUser = LoginUser.builder().id(1L).username("alice").tokenId("jti-old").build();
        when(jwtUtils.parseToken("old")).thenReturn(oldUser);
        when(stringRedisTemplate.hasKey(RedisKeys.TOKEN_BLACKLIST + "jti-old")).thenReturn(false);
        when(userMapper.selectById(1L)).thenReturn(user());
        when(userService.buildUserInfo(any(SysUser.class))).thenReturn(userInfo());
        when(jwtUtils.generateToken(any(LoginUser.class))).thenAnswer(invocation -> {
            LoginUser loginUser = invocation.getArgument(0);
            loginUser.setTokenId("jti-new");
            return "token";
        });

        LoginVO result = authService.refresh("old");

        assertEquals("token", result.getToken());
        verify(tokenSessionService).revoke(1L, "jti-old");
        verify(tokenSessionService).register(1L, "jti-new");
    }

    @Test
    void logoutShouldRevokeCurrentToken() {
        LoginUser loginUser = LoginUser.builder().id(1L).username("alice").tokenId("jti-1").build();
        when(jwtUtils.parseToken("t")).thenReturn(loginUser);

        authService.logout("t");

        verify(tokenSessionService).revoke(1L, "jti-1");
    }
}
