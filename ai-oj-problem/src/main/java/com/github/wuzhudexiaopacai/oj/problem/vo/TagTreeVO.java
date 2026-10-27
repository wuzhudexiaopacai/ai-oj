package com.github.wuzhudexiaopacai.oj.problem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "标签分类树节点（分类 → 标签）")
public class TagTreeVO implements Serializable {

    /**
     * 分类 ID
     */
    @Schema(description = "标签分类 ID")
    private Long categoryId;

    /**
     * 分类名
     */
    @Schema(description = "标签分类名称，如 算法")
    private String categoryName;

    /**
     * 排序值
     */
    @Schema(description = "分类排序值，按升序展示")
    private Integer sort;

    /**
     * 该分类下的标签
     */
    @Schema(description = "该分类下的标签列表，无标签时为空列表")
    private List<TagVO> tags;
}
