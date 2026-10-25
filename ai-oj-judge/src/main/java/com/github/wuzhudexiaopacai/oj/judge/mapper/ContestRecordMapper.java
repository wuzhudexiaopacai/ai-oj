package com.github.wuzhudexiaopacai.oj.judge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wuzhudexiaopacai.oj.judge.entity.ContestRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 比赛成绩 Mapper
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Mapper
public interface ContestRecordMapper extends BaseMapper<ContestRecord> {
}
