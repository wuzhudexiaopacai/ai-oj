package com.github.wuzhudexiaopacai.oj.judge.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 提交详情 VO（含用例明细）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class SubmissionDetailVO implements Serializable {

    private Long id;

    private String submitId;

    private Long userId;

    private Long problemId;

    private Long contestId;

    private String language;

    private String code;

    private Integer codeLength;

    private Integer status;

    private String statusDesc;

    private Integer score;

    private Integer timeUsed;

    private Integer memoryUsed;

    private String errorMessage;

    private LocalDateTime judgeTime;

    private LocalDateTime createTime;

    /**
     * 判题用例明细
     */
    private List<CaseResultItem> cases;

    /**
     * 单用例结果
     */
    @Data
    public static class CaseResultItem implements Serializable {

        private Integer seq;

        private Integer status;

        private String statusDesc;

        private Integer timeUsed;

        private Integer memoryUsed;

        private Integer score;

        private String outputPreview;
    }
}
