package com.github.wuzhudexiaopacai.oj.problem.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 题目列表项
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class ProblemPageVO implements Serializable {

    private Long id;

    private String problemKey;

    private String title;

    /**
     * 难度：1简单 2中等 3困难
     */
    private Integer difficulty;

    /**
     * 标签列表
     */
    private List<TagVO> tags;

    private Integer acceptCount;

    private Integer submitCount;

    /**
     * 通过率（百分比，submitCount=0 时为 0）
     */
    private BigDecimal passRate;

    /**
     * 当前用户完成状态（本接口保持无状态，恒为 null；
     * 登录用户的通过状态由调用方走 submission 服务接口获取后自行合并）
     */
    private Integer userStatus;
}
