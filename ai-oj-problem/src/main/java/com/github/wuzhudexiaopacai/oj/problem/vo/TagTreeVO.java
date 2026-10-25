package com.github.wuzhudexiaopacai.oj.problem.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 标签分类树节点（category → tags）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class TagTreeVO implements Serializable {

    /**
     * 分类 ID
     */
    private Long categoryId;

    /**
     * 分类名
     */
    private String categoryName;

    /**
     * 排序值
     */
    private Integer sort;

    /**
     * 该分类下的标签
     */
    private List<TagVO> tags;
}
