package com.github.wuzhudexiaopacai.oj.contest.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 比赛排行榜展示对象
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class ContestRankVO implements Serializable {

    /**
     * 名次（从 1 开始）
     */
    private Integer rank;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名（成绩记录快照 / 用户服务补充）
     */
    private String username;

    /**
     * 昵称（用户服务批量补充）
     */
    private String nickname;

    /**
     * 头像（用户服务批量补充）
     */
    private String avatar;

    /**
     * 个人简介（用户服务批量补充，前端超长截断展示）
     */
    private String intro;

    /**
     * 通过题数（ACM 赛制）
     */
    private Integer acceptCount;

    /**
     * ACM 罚时（秒）
     */
    private Long penaltySeconds;

    /**
     * 总分（OI 赛制）
     */
    private Integer totalScore;
}
