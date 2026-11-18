package com.github.wuzhudexiaopacai.oj.judge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
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

    /**
     * 统计用户在指定比赛内的最高分之和（OI 赛制：每题取最好成绩，重复提交不累加）
     *
     * @param contestId 比赛 ID
     * @param userId    用户 ID
     * @return 最高分之和
     */
    @Select("SELECT COALESCE(SUM(t.best_score), 0) FROM ("
            + "SELECT MAX(score) AS best_score FROM judge_submission "
            + "WHERE contest_id = #{contestId} AND user_id = #{userId} AND deleted = 0 "
            + "GROUP BY problem_id) t")
    Integer selectBestScoreSumByContest(@Param("contestId") Long contestId,
                                        @Param("userId") Long userId);

    /**
     * 统计用户在指定比赛内通过的题目数（仅严格 AC，与个人/题目通过统计口径一致）
     *
     * @param contestId 比赛 ID
     * @param userId    用户 ID
     * @return 通过题数
     */
    @Select("SELECT COUNT(DISTINCT problem_id) FROM judge_submission "
            + "WHERE contest_id = #{contestId} AND user_id = #{userId} "
            + "AND status = 3 AND deleted = 0")
    Integer selectSolvedCountByContest(@Param("contestId") Long contestId,
                                       @Param("userId") Long userId);

    /**
     * 查询用户在指定比赛内每道题首次通过的时间（用于按题累加罚时，仅严格 AC）
     *
     * @param contestId 比赛 ID
     * @param userId    用户 ID
     * @return 每道题首次通过时间
     */
    @Select("SELECT MIN(create_time) FROM judge_submission "
            + "WHERE contest_id = #{contestId} AND user_id = #{userId} "
            + "AND status = 3 AND deleted = 0 GROUP BY problem_id")
    List<LocalDateTime> selectFirstAcceptedTimesByContest(@Param("contestId") Long contestId,
                                                          @Param("userId") Long userId);
}

