package com.github.wuzhudexiaopacai.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 题解实体（pms_solution）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_solution")
public class Solution extends BaseEntity {

    /**
     * 题目 ID
     */
    private Long problemId;

    /**
     * 作者用户 ID
     */
    private Long userId;

    /**
     * 题解标题
     */
    private String title;

    /**
     * Markdown 内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 状态：1正常 0待审核
     */
    private Integer status;
}
