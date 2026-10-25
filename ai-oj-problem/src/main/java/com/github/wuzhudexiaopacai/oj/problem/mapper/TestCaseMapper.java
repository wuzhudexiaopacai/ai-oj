package com.github.wuzhudexiaopacai.oj.problem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wuzhudexiaopacai.oj.problem.entity.TestCase;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 评测用例 Mapper
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface TestCaseMapper extends BaseMapper<TestCase> {

    /** 物理删除题目的评测用例（避免逻辑删除的残留行累积） */
    @Delete("DELETE FROM pms_test_case WHERE problem_id = #{problemId}")
    int deletePhysicalByProblemId(@Param("problemId") Long problemId);
}
