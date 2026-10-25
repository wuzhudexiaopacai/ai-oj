package com.github.wuzhudexiaopacai.oj.contest.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 比赛题目展示对象
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class ContestProblemVO implements Serializable {

    /**
     * 题目 ID
     */
    private Long problemId;

    /**
     * 赛中题号，如 A
     */
    private String displayId;

    /**
     * 赛中展示标题（空则前端回退展示原题标题）
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

    /**
     * 当前用户在该比赛内的做题状态：AC=已通过，ATTEMPTED=尝试过，为空=未提交
     * <p>
     * 按 contestId 独立计算，不含赛前历史提交；未登录时为空。
     */
    private String status;
}
