package com.github.wuzhudexiaopacai.oj.problem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wuzhudexiaopacai.oj.problem.entity.Problem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 题目 Mapper
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface ProblemMapper extends BaseMapper<Problem> {

    /**
     * 提交数 +1，通过数 +acceptDelta（0 或 1），由判题结果消费者调用
     *
     * @param id          题目 ID
     * @param acceptDelta 通过增量
     * @return 影响行数
     */
    @Update("UPDATE pms_problem SET submit_count = submit_count + 1, "
            + "accept_count = accept_count + #{acceptDelta} WHERE id = #{id}")
    int incrementStat(@Param("id") Long id, @Param("acceptDelta") int acceptDelta);
}
