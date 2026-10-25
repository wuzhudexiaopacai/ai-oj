package com.github.wuzhudexiaopacai.oj.problem.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 题目分页查询条件
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class ProblemPageQueryDTO implements Serializable {

    /**
     * 页码
     */
    private long pageNum = 1;

    /**
     * 每页大小
     */
    private long pageSize = 20;

    /**
     * 关键字（标题/题号模糊）
     */
    private String keyword;

    /**
     * 难度：1简单 2中等 3困难
     */
    private Integer difficulty;

    /**
     * 标签 ID 多选（需同时满足，逐个 exists 子查询）
     */
    private List<Long> tagIds;
}
