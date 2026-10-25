package com.github.wuzhudexiaopacai.oj.contest.vo;

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
public class ContestPageVO implements Serializable {

    /**
     * 比赛 ID
     */
    private Long id;

    /**
     * 比赛标题
     */
    private String title;

    /**
     * 比赛简介（列表卡片展示）
     */
    private String description;

    /**
     * 赛制：0 ACM 1 OI
     */
    private Integer type;

    /**
     * 状态机：SCHEDULED/RUNNING/ENDED/CANCELED
     */
    private String status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 封榜时间
     */
    private LocalDateTime sealTime;

    /**
     * 报名人数
     */
    private Integer registerCount;
}
