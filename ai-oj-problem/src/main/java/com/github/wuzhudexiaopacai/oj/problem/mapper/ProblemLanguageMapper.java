package com.github.wuzhudexiaopacai.oj.problem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wuzhudexiaopacai.oj.problem.entity.ProblemLanguage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 题目语言模板 Mapper
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface ProblemLanguageMapper extends BaseMapper<ProblemLanguage> {

    /**
     * 物理删除题目的语言模板。
     * BaseMapper.delete 走逻辑删除，旧行仍占用 (problem_id, language) 唯一键，
     * 编辑题目重建模板时会报 Duplicate entry，因此子表重建必须真删。
     */
    @Delete("DELETE FROM pms_problem_language WHERE problem_id = #{problemId}")
    int deletePhysicalByProblemId(@Param("problemId") Long problemId);
}
