package com.github.wuzhudexiaopacai.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 标签实体（pms_tag）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_tag")
public class Tag extends BaseEntity {

    /**
     * 分类 ID
     */
    private Long categoryId;

    /**
     * 标签名
     */
    private String name;

    /**
     * 展示颜色
     */
    private String color;
}
