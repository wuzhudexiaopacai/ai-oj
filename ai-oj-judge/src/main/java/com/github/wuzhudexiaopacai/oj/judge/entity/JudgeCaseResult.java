package com.github.wuzhudexiaopacai.oj.judge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 单用例判题结果明细
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("judge_case_result")
public class JudgeCaseResult extends BaseEntity {

    /**
     * 提交记录 ID
     */
    private Long submissionId;

    /**
     * 用例序号
     */
    private Integer seq;

    /**
     * 该用例状态码（JudgeStatusEnum.code）
     */
    private Integer status;

    /**
     * 耗时（毫秒）
     */
    private Integer timeUsed;

    /**
     * 内存（KB）
     */
    private Integer memoryUsed;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 实际输出预览（截断）
     */
    private String outputPreview;
}
