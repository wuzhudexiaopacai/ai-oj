package com.github.wuzhudexiaopacai.oj.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wuzhudexiaopacai.oj.contest.entity.ContestRegistration;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 比赛报名 Mapper
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface ContestRegistrationMapper extends BaseMapper<ContestRegistration> {

    /**
     * 物理删除报名记录（绕过逻辑删除：uk_contest_user 唯一约束要求取消后可再次报名，
     * 若逻辑删除残留数据行会导致重新报名触发唯一键冲突）
     *
     * @param contestId 比赛 ID
     * @param userId    用户 ID
     * @return 影响行数
     */
    @Delete("DELETE FROM contest_registration WHERE contest_id = #{contestId} AND user_id = #{userId}")
    int physicalDelete(@Param("contestId") Long contestId, @Param("userId") Long userId);
}
