package com.github.wuzhudexiaopacai.oj.problem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wuzhudexiaopacai.oj.problem.entity.ProblemTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 题目-标签关联 Mapper
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface ProblemTagMapper extends BaseMapper<ProblemTag> {

    /** 物理删除题目的标签关联（避免逻辑删除的残留行累积） */
    @Delete("DELETE FROM pms_problem_tag WHERE problem_id = #{problemId}")
    int deletePhysicalByProblemId(@Param("problemId") Long problemId);
}
