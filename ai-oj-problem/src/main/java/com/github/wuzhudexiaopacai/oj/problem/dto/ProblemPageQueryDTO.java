package com.github.wuzhudexiaopacai.oj.problem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "题目分页查询条件")
public class ProblemPageQueryDTO implements Serializable {

    /**
     * 页码
     */
    @Schema(description = "页码，从 1 开始，默认 1", example = "1")
    private long pageNum = 1;

    /**
     * 每页大小
     */
    @Schema(description = "每页条数，默认 20", example = "20")
    private long pageSize = 20;

    /**
     * 关键字（标题/题号模糊）
     */
    @Schema(description = "关键字，按标题或题号模糊匹配，不传则不过滤", example = "两数之和")
    private String keyword;

    /**
     * 难度：1简单 2中等 3困难
     */
    @Schema(description = "难度：0简单 1中等 2困难，不传则不过滤", example = "1")
    private Integer difficulty;

    /**
     * 标签 ID 多选（需同时满足，逐个 exists 子查询）
     */
    @Schema(description = "标签 ID 列表，多选时要求题目同时具备全部所选标签；不传则不过滤", example = "[1,2]")
    private List<Long> tagIds;
}
