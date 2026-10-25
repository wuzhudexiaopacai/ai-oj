package com.github.wuzhudexiaopacai.oj.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysRolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 角色-权限关联 Mapper
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 物理删除指定角色的全部关联（重绑权限场景，避免唯一键 uk_role_perm 与逻辑删除冲突）
     *
     * @param roleId 角色ID
     * @return 删除行数
     */
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int physicalDeleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 物理删除指定权限的全部关联（权限删除场景）
     *
     * @param permissionId 权限ID
     * @return 删除行数
     */
    @Delete("DELETE FROM sys_role_permission WHERE permission_id = #{permissionId}")
    int physicalDeleteByPermissionId(@Param("permissionId") Long permissionId);
}
