package com.github.wuzhudexiaopacai.oj.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.wuzhudexiaopacai.oj.auth.dto.PermissionSaveRequest;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysPermission;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysRolePermission;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysPermissionMapper;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysRolePermissionMapper;
import com.github.wuzhudexiaopacai.oj.auth.service.PermissionService;
import com.github.wuzhudexiaopacai.oj.auth.service.UserService;
import com.github.wuzhudexiaopacai.oj.auth.vo.PermissionVO;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 权限服务实现
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysPermissionMapper permissionMapper;

    private final SysRolePermissionMapper rolePermissionMapper;

    private final UserService userService;

    @Override
    public List<PermissionVO> tree() {
        List<SysPermission> all = permissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery()
                .orderByAsc(SysPermission::getSort)
                .orderByAsc(SysPermission::getId));
        List<PermissionVO> voList = all.stream().map(this::toVO).collect(Collectors.toList());
        Map<Long, PermissionVO> voMap = voList.stream()
                .collect(Collectors.toMap(PermissionVO::getId, Function.identity()));
        List<PermissionVO> roots = new ArrayList<>();
        for (PermissionVO vo : voList) {
            PermissionVO parent = vo.getParentId() == null ? null : voMap.get(vo.getParentId());
            if (parent == null) {
                // 根节点或父权限缺失时按根节点展示，保证可见可维护
                roots.add(vo);
            } else {
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(vo);
            }
        }
        return roots;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createPermission(PermissionSaveRequest request) {
        checkCodeUnique(request.getCode(), null);
        checkParentExists(request.getParentId());
        SysPermission permission = new SysPermission();
        fillEntity(permission, request);
        permissionMapper.insert(permission);
        return permission.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updatePermission(PermissionSaveRequest request) {
        Asserts.notNull(request.getId(), "权限ID不能为空");
        SysPermission exist = permissionMapper.selectById(request.getId());
        Asserts.notNull(exist, "权限不存在");
        Asserts.isTrue(!Objects.equals(request.getId(), request.getParentId()), "父权限不能是自身");
        checkCodeUnique(request.getCode(), exist.getId());
        checkParentExists(request.getParentId());
        SysPermission update = new SysPermission();
        update.setId(exist.getId());
        fillEntity(update, request);
        permissionMapper.updateById(update);
        evictAffectedUsers(exist.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deletePermission(Long id) {
        SysPermission exist = permissionMapper.selectById(id);
        Asserts.notNull(exist, "权限不存在");
        Long children = permissionMapper.selectCount(Wrappers.<SysPermission>lambdaQuery()
                .eq(SysPermission::getParentId, id));
        Asserts.isTrue(children == 0, "请先删除子权限");
        // 先失效受影响用户缓存，再清理关联
        evictAffectedUsers(id);
        permissionMapper.deleteById(id);
        rolePermissionMapper.physicalDeleteByPermissionId(id);
    }

    /**
     * 失效绑定了该权限的所有角色下的用户缓存
     */
    private void evictAffectedUsers(Long permissionId) {
        List<Long> roleIds = rolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery()
                        .eq(SysRolePermission::getPermissionId, permissionId))
                .stream().map(SysRolePermission::getRoleId).distinct().toList();
        roleIds.forEach(userService::evictUserCacheByRoleId);
    }

    private void checkCodeUnique(String code, Long excludeId) {
        Long count = permissionMapper.selectCount(Wrappers.<SysPermission>lambdaQuery()
                .eq(SysPermission::getCode, code)
                .ne(excludeId != null, SysPermission::getId, excludeId));
        Asserts.isTrue(count == 0, "权限标识已存在");
    }

    private void checkParentExists(Long parentId) {
        if (parentId != null && parentId != 0) {
            Asserts.notNull(permissionMapper.selectById(parentId), "父权限不存在");
        }
    }

    private void fillEntity(SysPermission permission, PermissionSaveRequest request) {
        permission.setParentId(request.getParentId());
        permission.setCode(request.getCode());
        permission.setName(request.getName());
        permission.setType(request.getType());
        permission.setPath(request.getPath() == null ? "" : request.getPath());
        permission.setMethod(request.getMethod() == null ? "" : request.getMethod());
        permission.setSort(request.getSort() == null ? 0 : request.getSort());
        permission.setStatus(request.getStatus());
    }

    private PermissionVO toVO(SysPermission permission) {
        PermissionVO vo = new PermissionVO();
        vo.setId(permission.getId());
        vo.setParentId(permission.getParentId());
        vo.setCode(permission.getCode());
        vo.setName(permission.getName());
        vo.setType(permission.getType());
        vo.setPath(permission.getPath());
        vo.setMethod(permission.getMethod());
        vo.setSort(permission.getSort());
        vo.setStatus(permission.getStatus());
        return vo;
    }
}
