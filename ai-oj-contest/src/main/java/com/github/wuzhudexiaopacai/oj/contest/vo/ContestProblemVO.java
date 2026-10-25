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
}
