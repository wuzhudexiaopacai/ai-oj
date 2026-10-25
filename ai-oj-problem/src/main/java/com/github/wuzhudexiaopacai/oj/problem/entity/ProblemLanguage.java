package com.github.wuzhudexiaopacai.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 题目多语言代码模板实体（pms_problem_language）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_problem_language")
public class ProblemLanguage extends BaseEntity {

    /**
     * 题目 ID
     */
    private Long problemId;

    /**
     * 语言：java/cpp/python/go...
     */
    private String language;

    /**
     * 初始代码模板
     */
    private String codeTemplate;
}
