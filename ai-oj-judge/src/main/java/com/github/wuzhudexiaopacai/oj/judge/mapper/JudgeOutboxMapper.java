package com.github.wuzhudexiaopacai.oj.judge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeOutbox;
import org.apache.ibatis.annotations.Mapper;

/**
 * 判题结果本地消息表 Mapper
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Mapper
public interface JudgeOutboxMapper extends BaseMapper<JudgeOutbox> {
}
