package com.github.wuzhudexiaopacai.oj.contest.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 比赛排行榜展示对象
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "比赛排行榜展示对象")
public class ContestRankVO implements Serializable {

    /**
     * 名次（从 1 开始）
     */
    @Schema(description = "名次，从 1 开始", example = "1")
    private Integer rank;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID", example = "10001")
    private Long userId;

    /**
     * 用户名（成绩记录快照 / 用户服务补充）
     */
    @Schema(description = "用户名，优先取成绩记录快照，缺省时由用户服务补充，可能为 null", example = "alice")
    private String username;

    /**
     * 昵称（用户服务批量补充）
     */
    @Schema(description = "昵称，由用户服务批量补充，用户服务不可用时为 null", example = "小艾")
    private String nickname;

    /**
     * 头像（用户服务批量补充）
     */
    @Schema(description = "头像 URL，由用户服务批量补充，可能为 null", example = "https://example.com/avatar.png")
    private String avatar;

    /**
     * 个人简介（用户服务批量补充，前端超长截断展示）
     */
    @Schema(description = "个人简介，由用户服务批量补充，前端超长截断展示，可能为 null", example = "ACM 爱好者")
    private String intro;

    /**
     * 通过题数（ACM 赛制）
     */
    @Schema(description = "通过题数（ACM 赛制），无数据时为 0", example = "5")
    private Integer acceptCount;

    /**
     * ACM 罚时（秒）
     */
    @Schema(description = "ACM 罚时，单位：秒（ACM 赛制），无数据时为 0", example = "3600")
    private Long penaltySeconds;

    /**
     * 总分（OI 赛制）
     */
    @Schema(description = "总分（OI 赛制），无数据时为 0", example = "400")
    private Integer totalScore;
}
