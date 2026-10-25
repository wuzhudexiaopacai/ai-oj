package com.github.wuzhudexiaopacai.oj.contest.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 比赛事件消息体（CONTEST_EVENT_TOPIC，tag=start|end 区分事件类型，
 * judge 服务消费组 ai-oj-contest-event-consumer 消费 end 事件回写 rank_snapshot）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class ContestEventMessage implements Serializable {

    /**
     * 事件类型：start 开赛 / end 结束
     */
    private String eventType;

    /**
     * 比赛 ID
     */
    private Long contestId;

    /**
     * 赛制：0 ACM 1 OI
     */
    private Integer contestType;

    /**
     * 事件时间戳（毫秒）
     */
    private Long timestamp;
}
