package com.github.wuzhudexiaopacai.oj.auth.service;

import com.github.wuzhudexiaopacai.oj.auth.dto.RolePageQueryRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.RoleSaveRequest;
import com.github.wuzhudexiaopacai.oj.auth.vo.RoleVO;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;

import java.util.List;

/**
 * 角色服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface RoleService {

    /**
     * 分页查询角色
     *
     * @param query 查询条件
     * @return 角色分页
     */
    PageResult<RoleVO> pageRoles(RolePageQueryRequest query);

    /**
     * 查询全部启用角色（下拉选择场景）
     *
     * @return 角色列表
     */
    List<RoleVO> listAll();

    /**
     * 角色详情（含已绑定权限ID列表）
     *
     * @param id 角色ID
     * @return 角色信息
     */
    RoleVO getRoleDetail(Long id);

    /**
     * 新增角色
     *
     * @param request 角色请求
     * @return 新角色ID
     */
    Long createRole(RoleSaveRequest request);

    /**
     * 编辑角色（编码不可变更；permissionIds 非空时全量替换权限绑定）
     *
     * @param request 角色请求
     */
    void updateRole(RoleSaveRequest request);

    /**
     * 删除角色（逻辑删除，并清理用户/权限关联）
     *
     * @param id 角色ID
     */
    void deleteRole(Long id);
}
