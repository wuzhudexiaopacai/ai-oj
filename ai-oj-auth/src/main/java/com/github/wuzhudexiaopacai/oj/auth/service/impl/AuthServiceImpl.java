package com.github.wuzhudexiaopacai.oj.auth.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.wuzhudexiaopacai.oj.auth.constant.AuthConstants;
import com.github.wuzhudexiaopacai.oj.auth.dto.LoginRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.RegisterRequest;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysRole;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysUser;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysUserRole;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysRoleMapper;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysUserMapper;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysUserRoleMapper;
import com.github.wuzhudexiaopacai.oj.auth.mq.LoginLogProducer;
import com.github.wuzhudexiaopacai.oj.auth.service.AuthService;
import com.github.wuzhudexiaopacai.oj.auth.service.UserService;
import com.github.wuzhudexiaopacai.oj.auth.vo.CaptchaVO;
import com.github.wuzhudexiaopacai.oj.auth.vo.LoginVO;
import com.github.wuzhudexiaopacai.oj.auth.vo.UserInfoVO;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.core.result.ResultCode;
import com.github.wuzhudexiaopacai.oj.common.core.util.JwtUtils;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现（验证码 + 登录/注册/刷新/登出 + JWT 签发与黑名单）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;

    private final SysRoleMapper roleMapper;

    private final SysUserRoleMapper userRoleMapper;

    private final PasswordEncoder passwordEncoder;

    private final StringRedisTemplate stringRedisTemplate;

    private final JwtUtils jwtUtils;

    private final UserService userService;

    private final LoginLogProducer loginLogProducer;

    /**
     * 令牌有效期（分钟），黑名单保留时长与之对齐，确保覆盖旧令牌剩余生命周期
     */
    @Value("${ai-oj.jwt.expire-minutes:720}")
    private long expireMinutes;

    @Override
    public CaptchaVO captcha() {
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(160, 60, 4, 30);
        String captchaKey = IdUtil.fastSimpleUUID();
        stringRedisTemplate.opsForValue().set(RedisKeys.CAPTCHA + captchaKey, lineCaptcha.getCode(),
                AuthConstants.CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);
        return new CaptchaVO(captchaKey, lineCaptcha.getImageBase64Data());
    }

    @Override
    public LoginVO login(LoginRequest request, String ip, String userAgent) {
        verifyCaptcha(request.getCaptchaKey(), request.getCaptchaCode());
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginLogProducer.send(null, request.getUsername(), ip, userAgent, 0, "用户名或密码错误");
            throw new BizException("用户名或密码错误");
        }
        if (!Objects.equals(user.getStatus(), AuthConstants.STATUS_ENABLE)) {
            loginLogProducer.send(user.getId(), user.getUsername(), ip, userAgent, 0, "账号已被禁用");
            throw new BizException("账号已被禁用，请联系管理员");
        }
        UserInfoVO userInfo = userService.buildUserInfo(user);
        String token = generateToken(user, userInfo);
        loginLogProducer.send(user.getId(), user.getUsername(), ip, userAgent, 1, "登录成功");
        return new LoginVO(token, userInfo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserInfoVO register(RegisterRequest request, String ip, String userAgent) {
        verifyCaptcha(request.getCaptchaKey(), request.getCaptchaCode());
        Long usernameCount = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.getUsername()));
        Asserts.isTrue(usernameCount == 0, "用户名已存在");
        Long emailCount = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getEmail, request.getEmail()));
        Asserts.isTrue(emailCount == 0, "邮箱已被注册");
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setNickname(StrUtil.isBlank(request.getNickname()) ? request.getUsername() : request.getNickname());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setAvatar("");
        user.setIntro("");
        user.setStatus(AuthConstants.STATUS_ENABLE);
        user.setAcceptCount(0);
        user.setSubmitCount(0);
        user.setRating(1500);
        userMapper.insert(user);
        // 绑定默认角色（角色不存在时跳过，由管理员后续分配）
        SysRole defaultRole = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getCode, AuthConstants.DEFAULT_ROLE_CODE));
        if (defaultRole != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(defaultRole.getId());
            userRoleMapper.insert(userRole);
        }
        loginLogProducer.send(user.getId(), user.getUsername(), ip, userAgent, 1, "注册成功");
        return userService.buildUserInfo(user);
    }

    @Override
    public LoginVO refresh(String token) {
        Asserts.notBlank(token, "未携带认证令牌");
        LoginUser oldUser = parseToken(token);
        Asserts.isTrue(!Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeys.TOKEN_BLACKLIST + oldUser.getTokenId())),
                "令牌已注销，请重新登录");
        SysUser user = userMapper.selectById(oldUser.getId());
        Asserts.notNull(user, "用户不存在");
        Asserts.isTrue(Objects.equals(user.getStatus(), AuthConstants.STATUS_ENABLE), "账号已被禁用，请联系管理员");
        // 旧令牌进黑名单，按数据库当前权限重新签发
        blacklist(oldUser.getTokenId());
        UserInfoVO userInfo = userService.buildUserInfo(user);
        return new LoginVO(generateToken(user, userInfo), userInfo);
    }

    @Override
    public void logout(String token) {
        if (StrUtil.isBlank(token)) {
            return;
        }
        try {
            LoginUser loginUser = jwtUtils.parseToken(token);
            blacklist(loginUser.getTokenId());
        } catch (JwtException | IllegalArgumentException e) {
            // 令牌已失效视为已登出，保持接口幂等
            log.debug("登出时令牌已失效：{}", e.getMessage());
        }
    }

    /**
     * 校验并消费图形验证码（一次性）
     */
    private void verifyCaptcha(String captchaKey, String captchaCode) {
        String cacheKey = RedisKeys.CAPTCHA + captchaKey;
        String code = stringRedisTemplate.opsForValue().get(cacheKey);
        stringRedisTemplate.delete(cacheKey);
        Asserts.notBlank(code, "验证码已过期，请重新获取");
        Asserts.isTrue(code.equalsIgnoreCase(captchaCode), "验证码错误");
    }

    /**
     * 解析令牌，失败统一抛 401
     */
    private LoginUser parseToken(String token) {
        try {
            return jwtUtils.parseToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
    }

    /**
     * 令牌加入黑名单（保留时长 = 令牌有效期，覆盖其剩余生命周期）
     */
    private void blacklist(String tokenId) {
        stringRedisTemplate.opsForValue().set(RedisKeys.TOKEN_BLACKLIST + tokenId, "1",
                expireMinutes, TimeUnit.MINUTES);
    }

    /**
     * 构建 LoginUser 并签发 JWT（权限 = 角色编码 + 权限标识）
     */
    private String generateToken(SysUser user, UserInfoVO userInfo) {
        List<String> authorities = new ArrayList<>(userInfo.getRoles());
        authorities.addAll(userInfo.getPermissions());
        LoginUser loginUser = LoginUser.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .authorities(authorities)
                .build();
        return jwtUtils.generateToken(loginUser);
    }
}
