package com.github.wuzhudexiaopacai.oj.auth.service;

import com.github.wuzhudexiaopacai.oj.api.dto.UserBriefDTO;
import com.github.wuzhudexiaopacai.oj.auth.dto.AssignRolesRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.PasswordUpdateRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.ProfileUpdateRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.UserAdminSaveRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.UserPageQueryRequest;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysUser;
import com.github.wuzhudexiaopacai.oj.auth.vo.UserAdminVO;
import com.github.wuzhudexiaopacai.oj.auth.vo.UserInfoVO;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;

import java.util.List;

/**
 * 用户服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface UserService {

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    UserInfoVO getCurrentUserInfo();

    /**
     * 获取用户信息（带缓存）
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserInfoVO getUserInfo(Long userId);

    /**
     * 构建用户信息（实时查询角色与权限，不走缓存）
     *
     * @param user 用户实体
     * @return 用户信息
     */
    UserInfoVO buildUserInfo(SysUser user);

    /**
     * 修改当前用户个人资料
     *
     * @param request 资料请求
     */
    void updateProfile(ProfileUpdateRequest request);

    /**
     * 修改当前用户密码
     *
     * @param request 密码请求
     */
    void updatePassword(PasswordUpdateRequest request);

    /**
     * 分页查询用户（管理端）
     *
     * @param query 查询条件
     * @return 用户分页
     */
    PageResult<UserAdminVO> pageUsers(UserPageQueryRequest query);

    /**
     * 用户详情（管理端，含角色ID列表）
     *
     * @param id 用户ID
     * @return 用户信息
     */
    UserAdminVO getUserDetail(Long id);

    /**
     * 新增用户（管理端）
     *
     * @param request 用户请求
     * @return 新用户ID
     */
    Long createUser(UserAdminSaveRequest request);

    /**
     * 编辑用户（管理端，登录名不可变更）
     *
     * @param request 用户请求
     */
    void updateUser(UserAdminSaveRequest request);

    /**
     * 删除用户（管理端，逻辑删除并清理角色关联）
     *
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 分配用户角色（管理端，全量替换）
     *
     * @param request 分配请求
     */
    void assignRoles(AssignRolesRequest request);

    /**
     * 批量获取用户简要信息（服务间调用）
     *
     * @param userIds 用户ID列表
     * @return 用户简要信息
     */
    List<UserBriefDTO> batchGetUserBrief(List<Long> userIds);

    /**
     * 失效指定用户的信息缓存
     *
     * @param userId 用户ID
     */
    void evictUserCache(Long userId);

    /**
     * 失效指定角色下全部用户的信息缓存（角色/权限变更场景）
     *
     * @param roleId 角色ID
     */
    void evictUserCacheByRoleId(Long roleId);
}
