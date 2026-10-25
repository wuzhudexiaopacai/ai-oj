package com.github.wuzhudexiaopacai.oj.auth.service;

import com.github.wuzhudexiaopacai.oj.auth.dto.PermissionSaveRequest;
import com.github.wuzhudexiaopacai.oj.auth.vo.PermissionVO;

import java.util.List;

/**
 * 权限服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface PermissionService {

    /**
     * 权限树（管理端）
     *
     * @return 树形权限列表
     */
    List<PermissionVO> tree();

    /**
     * 新增权限
     *
     * @param request 权限请求
     * @return 新权限ID
     */
    Long createPermission(PermissionSaveRequest request);

    /**
     * 编辑权限
     *
     * @param request 权限请求
     */
    void updatePermission(PermissionSaveRequest request);

    /**
     * 删除权限（存在子权限时拒绝，逻辑删除并清理角色关联）
     *
     * @param id 权限ID
     */
    void deletePermission(Long id);
}
