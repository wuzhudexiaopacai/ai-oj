package com.github.wuzhudexiaopacai.oj.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.wuzhudexiaopacai.oj.auth.constant.AuthConstants;
import com.github.wuzhudexiaopacai.oj.auth.dto.RolePageQueryRequest;
import com.github.wuzhudexiaopacai.oj.auth.dto.RoleSaveRequest;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysPermission;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysRole;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysRolePermission;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysPermissionMapper;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysRoleMapper;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysRolePermissionMapper;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysUserRoleMapper;
import com.github.wuzhudexiaopacai.oj.auth.service.RoleService;
import com.github.wuzhudexiaopacai.oj.auth.service.UserService;
import com.github.wuzhudexiaopacai.oj.auth.vo.RoleVO;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色服务实现
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper roleMapper;

    private final SysUserRoleMapper userRoleMapper;

    private final SysRolePermissionMapper rolePermissionMapper;

    private final SysPermissionMapper permissionMapper;

    private final UserService userService;

    @Override
    public PageResult<RoleVO> pageRoles(RolePageQueryRequest query) {
        Page<SysRole> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<SysRole> resultPage = roleMapper.selectPage(page, Wrappers.<SysRole>lambdaQuery()
                .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                        .like(SysRole::getCode, query.getKeyword())
                        .or().like(SysRole::getName, query.getKeyword()))
                .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                .orderByAsc(SysRole::getId));
        return PageResult.of(resultPage, role -> toVO(role, null));
    }

    @Override
    public List<RoleVO> listAll() {
        List<SysRole> roles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getStatus, AuthConstants.STATUS_ENABLE)
                .orderByAsc(SysRole::getId));
        return roles.stream().map(role -> toVO(role, null)).toList();
    }

    @Override
    public RoleVO getRoleDetail(Long id) {
        SysRole role = roleMapper.selectById(id);
        Asserts.notNull(role, "角色不存在");
        List<Long> permissionIds = rolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery()
                        .eq(SysRolePermission::getRoleId, id))
                .stream().map(SysRolePermission::getPermissionId).toList();
        return toVO(role, permissionIds);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createRole(RoleSaveRequest request) {
        Long count = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, request.getCode()));
        Asserts.isTrue(count == 0, "角色编码已存在");
        SysRole role = new SysRole();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription() == null ? "" : request.getDescription());
        role.setStatus(request.getStatus());
        roleMapper.insert(role);
        if (request.getPermissionIds() != null) {
            replaceRolePermissions(role.getId(), request.getPermissionIds());
        }
        return role.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateRole(RoleSaveRequest request) {
        Asserts.notNull(request.getId(), "角色ID不能为空");
        SysRole exist = roleMapper.selectById(request.getId());
        Asserts.notNull(exist, "角色不存在");
        // 角色编码作为权限上下文标识，不允许变更
        SysRole update = new SysRole();
        update.setId(exist.getId());
        update.setName(request.getName());
        update.setDescription(request.getDescription() == null ? "" : request.getDescription());
        update.setStatus(request.getStatus());
        roleMapper.updateById(update);
        if (request.getPermissionIds() != null) {
            replaceRolePermissions(exist.getId(), request.getPermissionIds());
        }
        // 角色状态/权限变更后，该角色下用户的缓存信息全部失效
        userService.evictUserCacheByRoleId(exist.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteRole(Long id) {
        SysRole exist = roleMapper.selectById(id);
        Asserts.notNull(exist, "角色不存在");
        // 先失效受影响用户缓存，再清理关联
        userService.evictUserCacheByRoleId(id);
        roleMapper.deleteById(id);
        rolePermissionMapper.physicalDeleteByRoleId(id);
        userRoleMapper.physicalDeleteByRoleId(id);
    }

    /**
     * 全量替换角色权限绑定
     */
    private void replaceRolePermissions(Long roleId, List<Long> permissionIds) {
        List<Long> distinctIds = permissionIds.stream().distinct().toList();
        if (!distinctIds.isEmpty()) {
            List<SysPermission> permissions = permissionMapper.selectBatchIds(distinctIds);
            Asserts.isTrue(permissions.size() == distinctIds.size(), "存在无效的权限ID");
        }
        rolePermissionMapper.physicalDeleteByRoleId(roleId);
        distinctIds.forEach(permissionId -> {
            SysRolePermission rolePermission = new SysRolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            rolePermissionMapper.insert(rolePermission);
        });
    }

    private RoleVO toVO(SysRole role, List<Long> permissionIds) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setCode(role.getCode());
        vo.setName(role.getName());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        vo.setPermissionIds(permissionIds);
        vo.setCreateTime(role.getCreateTime());
        return vo;
    }
}
