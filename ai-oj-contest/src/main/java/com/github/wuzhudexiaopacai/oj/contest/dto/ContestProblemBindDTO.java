package com.github.wuzhudexiaopacai.oj.contest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 比赛题目绑定入参（display_id 由服务端按已有题目数量自动生成 A/B/C...）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class ContestProblemBindDTO implements Serializable {

    /**
     * 题目 ID
     */
    @NotNull(message = "题目ID不能为空")
    private Long problemId;

    /**
     * 赛中展示标题（可覆盖原题，空则展示原题标题）
     */
    @Size(max = 128, message = "展示标题长度不能超过128")
    private String title;

    /**
     * OI 模式分值
     */
    private Integer score;
}
