package com.github.wuzhudexiaopacai.oj.contest.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 比赛题目实体（contest_problem，display_id 为赛中题号 A/B/C...）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contest_problem")
public class ContestProblem extends BaseEntity {

    /**
     * 比赛 ID
     */
    private Long contestId;

    /**
     * 题目 ID
     */
    private Long problemId;

    /**
     * 赛中题号，如 A
     */
    private String displayId;

    /**
     * 赛中展示标题（可覆盖原题）
     */
    private String title;

    /**
     * OI 模式分值
     */
    private Integer score;

    /**
     * 排序
     */
    private Integer sort;
}
