package com.github.wuzhudexiaopacai.oj.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 比赛成绩记录（判题服务 → 比赛服务排行榜数据源）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class ContestRecordDTO implements Serializable {

    private Long contestId;

    private Long userId;

    /**
     * 用户名快照（排行榜展示免回查）
     */
    private String username;

    private Integer totalScore;

    private Integer acceptCount;

    /**
     * ACM 罚时（秒）
     */
    private Long penaltySeconds;
}
