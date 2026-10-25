package com.github.wuzhudexiaopacai.oj.api.dto;

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
public class ContestRuntimeDTO implements Serializable {

    private Long contestId;

    /**
     * 状态机：SCHEDULED/RUNNING/ENDED/CANCELED
     */
    private String status;

    /**
     * 赛制：0 ACM 1 OI
     */
    private Integer type;

    /**
     * 开始时间（epoch 毫秒，避免跨服务 LocalDateTime 序列化差异）
     */
    private Long startTimeMillis;

    /**
     * 结束时间（epoch 毫秒）
     */
    private Long endTimeMillis;

    /**
     * 比赛包含的题目 ID
     */
    private List<Long> problemIds = new ArrayList<>();

    /**
     * 查询时传入 userId 才有值：该用户是否已报名
     */
    private Boolean registered;
}
