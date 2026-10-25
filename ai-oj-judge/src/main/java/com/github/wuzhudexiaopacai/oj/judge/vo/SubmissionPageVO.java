package com.github.wuzhudexiaopacai.oj.judge.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提交分页 VO
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class SubmissionPageVO implements Serializable {

    private Long id;

    private String submitId;

    private Long problemId;

    /**
     * 展示题号（由题目服务回填）
     */
    private String problemKey;

    /**
     * 题目标题（由题目服务回填）
     */
    private String problemTitle;

    private Long contestId;

    private String language;

    private Integer status;

    private String statusDesc;

    private Integer score;

    private Integer timeUsed;

    private Integer memoryUsed;

    private LocalDateTime judgeTime;

    private LocalDateTime createTime;
}
