package com.github.wuzhudexiaopacai.oj.contest.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 比赛时间快照（WebSocket 实时推送体，前端倒计时基准）
 * <p>
 * 统一使用 epoch 毫秒下发，避免 LocalDateTime 字符串在不同时区浏览器的解析差异；
 * serverTimeMillis 用于前端校正本地时钟偏差。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "比赛时间快照，WebSocket 实时推送体，统一使用 epoch 毫秒")
public class ContestTimeVO implements Serializable {

    /**
     * 比赛 ID
     */
    @Schema(description = "比赛 ID", example = "1")
    private Long contestId;

    /**
     * 状态机：SCHEDULED/RUNNING/ENDED/CANCELED
     */
    @Schema(description = "比赛状态：SCHEDULED 未开始 / RUNNING 进行中 / ENDED 已结束 / CANCELED 已取消", example = "RUNNING")
    private String status;

    /**
     * 开始时间（epoch 毫秒）
     */
    @Schema(description = "开始时间，epoch 毫秒", example = "1793530800000")
    private Long startTimeMillis;

    /**
     * 结束时间（epoch 毫秒）
     */
    @Schema(description = "结束时间，epoch 毫秒", example = "1793541600000")
    private Long endTimeMillis;

    /**
     * 封榜时间（epoch 毫秒，可为空）
     */
    @Schema(description = "封榜时间，epoch 毫秒；未设置封榜时为 null", example = "1793538000000")
    private Long sealTimeMillis;

    /**
     * 服务端当前时间（epoch 毫秒），前端据此校正时钟偏差
     */
    @Schema(description = "服务端当前时间，epoch 毫秒，前端据此校正本地时钟偏差", example = "1793530800000")
    private Long serverTimeMillis;
}
