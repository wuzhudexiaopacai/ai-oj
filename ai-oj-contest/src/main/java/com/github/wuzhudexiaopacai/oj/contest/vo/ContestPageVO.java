package com.github.wuzhudexiaopacai.oj.contest.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 比赛分页展示对象
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "比赛分页展示对象")
public class ContestPageVO implements Serializable {

    /**
     * 比赛 ID
     */
    @Schema(description = "比赛 ID", example = "1")
    private Long id;

    /**
     * 比赛标题
     */
    @Schema(description = "比赛标题", example = "2026 春季新手赛")
    private String title;

    /**
     * 比赛简介（列表卡片展示）
     */
    @Schema(description = "比赛简介，列表卡片展示，可能为 null", example = "本场比赛为 ACM 赛制，共 6 题")
    private String description;

    /**
     * 赛制：0 ACM 1 OI
     */
    @Schema(description = "赛制：0 ACM / 1 OI", example = "0")
    private Integer type;

    /**
     * 状态机：SCHEDULED/RUNNING/ENDED/CANCELED
     */
    @Schema(description = "比赛状态：SCHEDULED 未开始 / RUNNING 进行中 / ENDED 已结束 / CANCELED 已取消", example = "RUNNING")
    private String status;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间，格式 yyyy-MM-dd HH:mm:ss", example = "2026-11-01 19:00:00")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间，格式 yyyy-MM-dd HH:mm:ss", example = "2026-11-01 22:00:00")
    private LocalDateTime endTime;

    /**
     * 封榜时间
     */
    @Schema(description = "封榜时间，格式 yyyy-MM-dd HH:mm:ss；未设置封榜时为 null", example = "2026-11-01 21:00:00")
    private LocalDateTime sealTime;

    /**
     * 报名人数
     */
    @Schema(description = "报名人数，无报名时为 0", example = "128")
    private Integer registerCount;
}
