package com.github.wuzhudexiaopacai.oj.contest.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 比赛详情展示对象（含题目列表；用户无关数据，可整体缓存）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "比赛详情展示对象，含比赛基础信息、报名统计与题目列表")
public class ContestDetailVO implements Serializable {

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
     * 比赛说明（Markdown）
     */
    @Schema(description = "比赛说明，支持 Markdown，可能为 null", example = "本场比赛为 ACM 赛制，共 6 题")
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
     * 创建者
     */
    @Schema(description = "创建者用户 ID", example = "1")
    private Long creatorId;

    /**
     * 可见性：1公开 2私有（密码不下发）
     */
    @Schema(description = "可见性：1 公开 / 2 私有；私有赛密码不会下发", example = "1")
    private Integer auth;

    /**
     * 是否已设置比赛密码（仅回显状态，不下发明文；编辑时留空表示不修改）
     */
    @Schema(description = "是否已设置比赛密码，仅回显状态、不下发明文；编辑时密码留空表示不修改", example = "true")
    private Boolean hasPassword;

    /**
     * 比赛题目列表（按 sort 升序）
     */
    @Schema(description = "比赛题目列表，按 sort 升序；比赛开始前或私有赛未报名时为空列表（管理员除外）")
    private List<ContestProblemVO> problems;

    /**
     * 已报名人数
     */
    @Schema(description = "已报名人数，无报名时为 0", example = "128")
    private Integer registerCount;

    /**
     * 当前登录用户是否已报名（未登录恒为 false）
     */
    @Schema(description = "当前登录用户是否已报名，未登录时恒为 false", example = "true")
    private Boolean registered;
}
