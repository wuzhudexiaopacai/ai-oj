package com.github.wuzhudexiaopacai.oj.judge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 判题提交 Mapper
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Mapper
public interface JudgeSubmissionMapper extends BaseMapper<JudgeSubmission> {

    /**
     * 查询用户已通过的题目 ID（ACCEPTED=3）
     *
     * @param userId 用户 ID
     * @return 题目 ID 列表
     */
    @Select("SELECT DISTINCT problem_id FROM judge_submission "
            + "WHERE user_id = #{userId} AND status = 3 AND deleted = 0")
    List<Long> selectSolvedProblemIds(@Param("userId") Long userId);

    /**
     * 查询用户提交过的题目 ID（含未通过）
     *
     * @param userId 用户 ID
     * @return 题目 ID 列表
     */
    @Select("SELECT DISTINCT problem_id FROM judge_submission "
            + "WHERE user_id = #{userId} AND deleted = 0")
    List<Long> selectAttemptedProblemIds(@Param("userId") Long userId);

    /**
     * 查询用户在指定比赛内已通过的题目 ID（比赛成绩按 contestId 独立计算）
     *
     * @param contestId 比赛 ID
     * @param userId    用户 ID
     * @return 题目 ID 列表
     */
    @Select("SELECT DISTINCT problem_id FROM judge_submission "
            + "WHERE contest_id = #{contestId} AND user_id = #{userId} AND status = 3 AND deleted = 0")
    List<Long> selectSolvedProblemIdsByContest(@Param("contestId") Long contestId,
                                               @Param("userId") Long userId);

    /**
     * 查询用户在指定比赛内提交过的题目 ID（含未通过）
     *
     * @param contestId 比赛 ID
     * @param userId    用户 ID
     * @return 题目 ID 列表
     */
    @Select("SELECT DISTINCT problem_id FROM judge_submission "
            + "WHERE contest_id = #{contestId} AND user_id = #{userId} AND deleted = 0")
    List<Long> selectAttemptedProblemIdsByContest(@Param("contestId") Long contestId,
                                                  @Param("userId") Long userId);

    /**
     * 查询比赛中有提交记录的用户 ID（排行榜只展示实际参赛的用户）
     *
     * @param contestId 比赛 ID
     * @return 用户 ID 列表
     */
    @Select("SELECT DISTINCT user_id FROM judge_submission "
            + "WHERE contest_id = #{contestId} AND deleted = 0")
    List<Long> selectUserIdsByContest(@Param("contestId") Long contestId);
}

