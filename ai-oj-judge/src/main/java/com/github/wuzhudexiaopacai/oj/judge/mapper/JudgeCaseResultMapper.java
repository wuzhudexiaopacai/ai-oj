package com.github.wuzhudexiaopacai.oj.judge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeCaseResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 判题用例结果 Mapper
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Mapper
public interface JudgeCaseResultMapper extends BaseMapper<JudgeCaseResult> {

    /**
     * 批量插入用例结果
     *
     * @param list 用例结果列表
     * @return 插入行数
     */
    int insertBatch(@Param("list") List<JudgeCaseResult> list);
}
