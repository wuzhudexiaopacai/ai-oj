package com.github.wuzhudexiaopacai.oj.contest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 比赛分页查询条件
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "比赛分页查询条件")
public class ContestPageQueryDTO implements Serializable {

    /**
     * 页码
     */
    @Schema(description = "页码，从 1 开始", defaultValue = "1", example = "1")
    private long pageNum = 1;

    /**
     * 每页大小
     */
    @Schema(description = "每页条数，默认 20", defaultValue = "20", example = "20")
    private long pageSize = 20;

    /**
     * 状态过滤：SCHEDULED/RUNNING/ENDED/CANCELED（空为不过滤）
     */
    @Schema(description = "状态过滤：SCHEDULED 未开始 / RUNNING 进行中 / ENDED 已结束 / CANCELED 已取消；留空不过滤", example = "RUNNING")
    private String status;

    /**
     * 赛制过滤：0 ACM 1 OI（空为不过滤）
     */
    @Schema(description = "赛制过滤：0 ACM / 1 OI；留空不过滤", example = "0")
    private Integer type;
}
