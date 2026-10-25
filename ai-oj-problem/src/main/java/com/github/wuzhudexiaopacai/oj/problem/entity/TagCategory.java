package com.github.wuzhudexiaopacai.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 标签分类实体（pms_tag_category）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_tag_category")
public class TagCategory extends BaseEntity {

    /**
     * 分类名，如 算法/数据结构
     */
    private String name;

    /**
     * 排序值
     */
    private Integer sort;
}
