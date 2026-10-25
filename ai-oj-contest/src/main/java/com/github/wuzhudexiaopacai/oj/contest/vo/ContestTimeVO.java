package com.github.wuzhudexiaopacai.oj.contest.vo;

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
public class ContestTimeVO implements Serializable {

    /**
     * 比赛 ID
     */
    private Long contestId;

    /**
     * 状态机：SCHEDULED/RUNNING/ENDED/CANCELED
     */
    private String status;

    /**
     * 开始时间（epoch 毫秒）
     */
    private Long startTimeMillis;

    /**
     * 结束时间（epoch 毫秒）
     */
    private Long endTimeMillis;

    /**
     * 封榜时间（epoch 毫秒，可为空）
     */
    private Long sealTimeMillis;

    /**
     * 服务端当前时间（epoch 毫秒），前端据此校正时钟偏差
     */
    private Long serverTimeMillis;
}
