package com.github.wuzhudexiaopacai.oj.judge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wuzhudexiaopacai.oj.judge.entity.ContestRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 比赛成绩 Mapper
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Mapper
public interface ContestRecordMapper extends BaseMapper<ContestRecord> {

    /**
     * 成绩单调合并：得分/通过数/罚时均从提交表重算，正常只会增大。
     * <p>
     * 使用 GREATEST 而非覆盖写，避免并发或乱序消息把已写入的成绩改小。
     *
     * @param id             成绩记录 ID
     * @param totalScore     重算后的总分
     * @param acceptCount    重算后的通过题数
     * @param penaltySeconds 重算后的罚时（秒）
     * @return 影响行数
     */
    @Update("UPDATE contest_record SET "
            + "total_score = GREATEST(total_score, #{totalScore}), "
            + "accept_count = GREATEST(accept_count, #{acceptCount}), "
            + "penalty_seconds = GREATEST(penalty_seconds, #{penaltySeconds}) "
            + "WHERE id = #{id} AND deleted = 0")
    int mergeScore(@Param("id") Long id,
                   @Param("totalScore") Integer totalScore,
                   @Param("acceptCount") Integer acceptCount,
                   @Param("penaltySeconds") Long penaltySeconds);
}
