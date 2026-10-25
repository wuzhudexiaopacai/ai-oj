package com.github.wuzhudexiaopacai.oj.contest.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 比赛分页查询条件
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class ContestPageQueryDTO implements Serializable {

    /**
     * 页码
     */
    private long pageNum = 1;

    /**
     * 每页大小
     */
    private long pageSize = 20;

    /**
     * 状态过滤：SCHEDULED/RUNNING/ENDED/CANCELED（空为不过滤）
     */
    private String status;

    /**
     * 赛制过滤：0 ACM 1 OI（空为不过滤）
     */
    private Integer type;
}
