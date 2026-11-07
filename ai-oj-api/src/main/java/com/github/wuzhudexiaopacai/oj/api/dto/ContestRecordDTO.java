package com.github.wuzhudexiaopacai.oj.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 比赛成绩记录（判题服务 → 比赛服务排行榜数据源）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "比赛成绩记录（内部 Feign 契约，比赛排行榜数据源）")
public class ContestRecordDTO implements Serializable {

    @Schema(description = "比赛 ID", example = "1")
    private Long contestId;

    @Schema(description = "用户 ID（雪花 ID）", example = "1")
    private Long userId;

    /**
     * 用户名快照（排行榜展示免回查）
     */
    @Schema(description = "用户名快照，排行榜展示免回查认证服务", example = "alice")
    private String username;

    @Schema(description = "OI 模式总分（各用例分值累加），ACM 模式不使用", example = "100")
    private Integer totalScore;

    @Schema(description = "通过题目数", example = "3")
    private Integer acceptCount;

    /**
     * ACM 罚时（秒）
     */
    @Schema(description = "ACM 罚时（秒）：首次通过时间 − 比赛开始时间 + 错误提交惩罚，OI 模式不使用", example = "1800")
    private Long penaltySeconds;

    /**
     * 结算名次快照（比赛结束时由判题服务按赛制结算写入，未结算为 null）
     */
    @Schema(description = "结算名次快照（比赛结束后写入，1 表示第一名）；未结算时为 null，实时榜单不使用该字段", example = "3")
    private Integer rankSnapshot;
}
