package com.github.wuzhudexiaopacai.oj.problem.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题解展示对象
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class SolutionVO implements Serializable {

    private Long id;

    private Long problemId;

    private Long userId;

    private String title;

    /**
     * 内容（列表接口不返回，详情返回）
     */
    private String content;

    private Integer likeCount;

    private LocalDateTime createTime;
}
