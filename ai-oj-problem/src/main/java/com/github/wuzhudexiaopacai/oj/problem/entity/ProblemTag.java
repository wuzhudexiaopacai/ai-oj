package com.github.wuzhudexiaopacai.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 题目-标签关联实体（pms_problem_tag）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_problem_tag")
public class ProblemTag extends BaseEntity {

    /**
     * 题目 ID
     */
    private Long problemId;

    /**
     * 标签 ID
     */
    private Long tagId;
}
