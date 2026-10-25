package com.github.wuzhudexiaopacai.oj.auth.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.wuzhudexiaopacai.oj.api.dto.UserBriefDTO;
import com.github.wuzhudexiaopacai.oj.auth.constant.AuthConstants;
import com.github.wuzhudexiaopacai.oj.auth.dto.AssignRolesRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.PasswordUpdateRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.ProfileUpdateRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.UserAdminSaveRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.UserPageQueryRequest;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysPermission;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysRole;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysRolePermission;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysUser;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysUserRole;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysPermissionMapper;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysRoleMapper;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysRolePermissionMapper;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysUserMapper;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysUserRoleMapper;
import com.github.wuzhudexiaopacai.oj.auth.service.UserService;
import com.github.wuzhudexiaopacai.oj.auth.vo.UserAdminVO;
import com.github.wuzhudexiaopacai.oj.auth.vo.UserInfoVO;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;

    private final SysUserRoleMapper userRoleMapper;

    private final SysRoleMapper roleMapper;

    private final SysRolePermissionMapper rolePermissionMapper;

    private final SysPermissionMapper permissionMapper;

    private final PasswordEncoder passwordEncoder;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public UserInfoVO getCurrentUserInfo() {
        LoginUser loginUser = SecurityUtils.requireLogin();
        return getUserInfo(loginUser.getId());
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        String cacheKey = RedisKeys.USER_INFO + userId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof UserInfoVO userInfoVO) {
            return userInfoVO;
        }
        SysUser user = userMapper.selectById(userId);
        Asserts.notNull(user, "用户不存在");
        UserInfoVO userInfo = buildUserInfo(user);
        redisTemplate.opsForValue().set(cacheKey, userInfo, AuthConstants.USER_INFO_CACHE_MINUTES, TimeUnit.MINUTES);
        return userInfo;
    }

    @Override
    public UserInfoVO buildUserInfo(SysUser user) {
        List<SysRole> roles = listEnabledRoles(user.getId());
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();
        List<String> permissionCodes = listEnabledPermissionCodes(roleIds);
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setIntro(user.getIntro());
        vo.setAcceptCount(user.getAcceptCount());
        vo.setSubmitCount(user.getSubmitCount());
        vo.setRating(user.getRating());
        vo.setRoles(roles.stream().map(SysRole::getCode).toList());
        vo.setPermissions(permissionCodes);
        return vo;
    }

    @Override
    public void updateProfile(ProfileUpdateRequest request) {
        LoginUser loginUser = SecurityUtils.requireLogin();
        SysUser user = userMapper.selectById(loginUser.getId());
        Asserts.notNull(user, "用户不存在");
        checkEmailUnique(request.getEmail(), user.getId());
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setNickname(request.getNickname());
        update.setEmail(request.getEmail());
        update.setAvatar(request.getAvatar() == null ? "" : request.getAvatar());
        update.setIntro(request.getIntro() == null ? "" : request.getIntro());
        userMapper.updateById(update);
        evictUserCache(user.getId());
    }

    @Override
    public void updatePassword(PasswordUpdateRequest request) {
        LoginUser loginUser = SecurityUtils.requireLogin();
        SysUser user = userMapper.selectById(loginUser.getId());
        Asserts.notNull(user, "用户不存在");
        Asserts.isTrue(passwordEncoder.matches(request.getOldPassword(), user.getPassword()), "原密码不正确");
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(update);
        // 已签发的 Token 到期前仍可用；如需强制下线可调用 /auth/logout
        evictUserCache(user.getId());
    }

    @Override
    public PageResult<UserAdminVO> pageUsers(UserPageQueryRequest query) {
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<SysUser> resultPage = userMapper.selectPage(page, Wrappers.<SysUser>lambdaQuery()
                .like(StrUtil.isNotBlank(query.getUsername()), SysUser::getUsername, query.getUsername())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .orderByDesc(SysUser::getCreateTime));
        Map<Long, List<Long>> roleMap = listRoleIdsByUserIds(
                resultPage.getRecords().stream().map(SysUser::getId).toList());
        return PageResult.of(resultPage, user -> toAdminVO(user, roleMap.getOrDefault(user.getId(), List.of())));
    }

    @Override
    public UserAdminVO getUserDetail(Long id) {
        SysUser user = userMapper.selectById(id);
        Asserts.notNull(user, "用户不存在");
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, id))
                .stream().map(SysUserRole::getRoleId).toList();
        return toAdminVO(user, roleIds);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createUser(UserAdminSaveRequest request) {
        Asserts.notBlank(request.getPassword(), "初始密码不能为空");
        checkUsernameUnique(request.getUsername());
        checkEmailUnique(request.getEmail(), null);
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setNickname(StrUtil.isBlank(request.getNickname()) ? request.getUsername() : request.getNickname());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setAvatar("");
        user.setIntro("");
        user.setStatus(request.getStatus());
        user.setAcceptCount(0);
        user.setSubmitCount(0);
        user.setRating(1500);
        userMapper.insert(user);
        if (request.getRoleIds() != null) {
            replaceUserRoles(user.getId(), request.getRoleIds());
        }
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateUser(UserAdminSaveRequest request) {
        Asserts.notNull(request.getId(), "用户ID不能为空");
        SysUser exist = userMapper.selectById(request.getId());
        Asserts.notNull(exist, "用户不存在");
        checkEmailUnique(request.getEmail(), exist.getId());
        SysUser update = new SysUser();
        update.setId(exist.getId());
        update.setNickname(StrUtil.isBlank(request.getNickname()) ? exist.getUsername() : request.getNickname());
        update.setEmail(request.getEmail());
        update.setStatus(request.getStatus());
        if (StrUtil.isNotBlank(request.getPassword())) {
            update.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userMapper.updateById(update);
        if (request.getRoleIds() != null) {
            replaceUserRoles(exist.getId(), request.getRoleIds());
        }
        evictUserCache(exist.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteUser(Long id) {
        SysUser exist = userMapper.selectById(id);
        Asserts.notNull(exist, "用户不存在");
        userMapper.deleteById(id);
        userRoleMapper.physicalDeleteByUserId(id);
        evictUserCache(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void assignRoles(AssignRolesRequest request) {
        SysUser exist = userMapper.selectById(request.getUserId());
        Asserts.notNull(exist, "用户不存在");
        replaceUserRoles(request.getUserId(), request.getRoleIds());
        evictUserCache(request.getUserId());
    }

    @Override
    public List<UserBriefDTO> batchGetUserBrief(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return List.of();
        }
        List<SysUser> users = userMapper.selectBatchIds(userIds.stream().distinct().toList());
        return users.stream().map(user -> {
            UserBriefDTO dto = new UserBriefDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
            dto.setAvatar(user.getAvatar());
            return dto;
        }).toList();
    }

    @Override
    public void evictUserCache(Long userId) {
        redisTemplate.delete(RedisKeys.USER_INFO + userId);
    }

    @Override
    public void evictUserCacheByRoleId(Long roleId) {
        List<Long> userIds = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getRoleId, roleId))
                .stream().map(SysUserRole::getUserId).toList();
        userIds.forEach(this::evictUserCache);
    }

    /**
     * 查询用户启用状态的角色
     */
    private List<SysRole> listEnabledRoles(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> Objects.equals(role.getStatus(), AuthConstants.STATUS_ENABLE))
                .toList();
    }

    /**
     * 查询角色集合下启用状态的权限标识
     */
    private List<String> listEnabledPermissionCodes(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery()
                        .in(SysRolePermission::getRoleId, roleIds))
                .stream().map(SysRolePermission::getPermissionId).distinct().toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectBatchIds(permissionIds).stream()
                .filter(permission -> Objects.equals(permission.getStatus(), AuthConstants.STATUS_ENABLE))
                .map(SysPermission::getCode)
                .distinct()
                .toList();
    }

    /**
     * 批量查询用户的角色ID映射（分页填充，避免 N+1）
     */
    private Map<Long, List<Long>> listRoleIdsByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .in(SysUserRole::getUserId, userIds))
                .stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId,
                        Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));
    }

    /**
     * 全量替换用户角色
     */
    private void replaceUserRoles(Long userId, List<Long> roleIds) {
        List<Long> distinctRoleIds = roleIds.stream().distinct().toList();
        if (!distinctRoleIds.isEmpty()) {
            List<SysRole> roles = roleMapper.selectBatchIds(distinctRoleIds);
            Asserts.isTrue(roles.size() == distinctRoleIds.size(), "存在无效的角色ID");
        }
        userRoleMapper.physicalDeleteByUserId(userId);
        distinctRoleIds.forEach(roleId -> {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        });
    }

    private void checkUsernameUnique(String username) {
        Long count = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        Asserts.isTrue(count == 0, "用户名已存在");
    }

    private void checkEmailUnique(String email, Long excludeUserId) {
        Long count = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getEmail, email)
                .ne(excludeUserId != null, SysUser::getId, excludeUserId));
        Asserts.isTrue(count == 0, "邮箱已被注册");
    }

    private UserAdminVO toAdminVO(SysUser user, List<Long> roleIds) {
        UserAdminVO vo = new UserAdminVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setIntro(user.getIntro());
        vo.setStatus(user.getStatus());
        vo.setAcceptCount(user.getAcceptCount());
        vo.setSubmitCount(user.getSubmitCount());
        vo.setRating(user.getRating());
        vo.setRoleIds(roleIds);
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }
}
