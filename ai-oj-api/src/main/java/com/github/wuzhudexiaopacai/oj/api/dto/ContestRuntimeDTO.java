package com.github.wuzhudexiaopacai.oj.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 比赛运行时信息（比赛服务 → 判题服务，提交校验用）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "比赛运行时信息（内部 Feign 契约，判题提交前置校验用）")
public class ContestRuntimeDTO implements Serializable {

    @Schema(description = "比赛 ID", example = "1")
    private Long contestId;

    /**
     * 状态机：SCHEDULED/RUNNING/ENDED/CANCELED
     */
    @Schema(description = "比赛状态：SCHEDULED 未开始 / RUNNING 进行中 / ENDED 已结束 / CANCELED 已取消", example = "RUNNING")
    private String status;

    /**
     * 赛制：0 ACM 1 OI
     */
    @Schema(description = "赛制：0 ACM / 1 OI", example = "0")
    private Integer type;

    /**
     * 开始时间（epoch 毫秒，避免跨服务 LocalDateTime 序列化差异）
     */
    @Schema(description = "开始时间（epoch 毫秒，避免跨服务 LocalDateTime 序列化差异）", example = "1735660800000")
    private Long startTimeMillis;

    /**
     * 结束时间（epoch 毫秒）
     */
    @Schema(description = "结束时间（epoch 毫秒）", example = "1735664400000")
    private Long endTimeMillis;

    /**
     * 比赛包含的题目 ID
     */
    @Schema(description = "比赛包含的题目 ID 列表", example = "[10001,10002]")
    private List<Long> problemIds = new ArrayList<>();

    /**
     * 查询时传入 userId 才有值：该用户是否已报名
     */
    @Schema(description = "该用户是否已报名；查询时未传 userId 则为 null", example = "true")
    private Boolean registered;
}
