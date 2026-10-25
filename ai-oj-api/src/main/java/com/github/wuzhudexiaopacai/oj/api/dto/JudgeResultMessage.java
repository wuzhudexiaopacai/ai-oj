package com.github.wuzhudexiaopacai.oj.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 判题结果消息（判题服务广播 → 题目统计/比赛成绩等消费方）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResultMessage implements Serializable {

    /**
     * 幂等提交标识
     */
    private String submitId;

    private Long userId;

    private Long problemId;

    /**
     * 比赛 ID（非比赛提交为 0）
     */
    private Long contestId;

    /**
     * 终态状态码（JudgeStatusEnum.code）
     */
    private Integer statusCode;

    private Integer score;

    /**
     * 最大耗时（毫秒）
     */
    private Integer timeUsed;

    /**
     * 最大内存（KB）
     */
    private Integer memoryUsed;

    private LocalDateTime judgeTime;
}
