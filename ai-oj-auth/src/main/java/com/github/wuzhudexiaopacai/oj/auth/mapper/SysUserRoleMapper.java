package com.github.wuzhudexiaopacai.oj.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 用户-角色关联 Mapper
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 物理删除指定用户的全部关联（重绑角色场景，避免唯一键 uk_user_role 与逻辑删除冲突）
     *
     * @param userId 用户ID
     * @return 删除行数
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int physicalDeleteByUserId(@Param("userId") Long userId);

    /**
     * 物理删除指定角色的全部关联（角色删除场景）
     *
     * @param roleId 角色ID
     * @return 删除行数
     */
    @Delete("DELETE FROM sys_user_role WHERE role_id = #{roleId}")
    int physicalDeleteByRoleId(@Param("roleId") Long roleId);
}
