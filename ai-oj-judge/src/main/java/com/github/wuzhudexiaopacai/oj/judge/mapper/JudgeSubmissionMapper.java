package com.github.wuzhudexiaopacai.oj.judge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 判题提交 Mapper
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Mapper
public interface JudgeSubmissionMapper extends BaseMapper<JudgeSubmission> {
}
