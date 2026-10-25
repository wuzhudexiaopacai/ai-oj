package com.github.wuzhudexiaopacai.oj.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 判题所需的题目信息（Feign 契约）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class ProblemJudgeDTO implements Serializable {

    private Long problemId;

    private String problemKey;

    private String title;

    /**
     * 时间限制（毫秒）
     */
    private Integer timeLimit;

    /**
     * 内存限制（MB）
     */
    private Integer memoryLimit;

    /**
     * 是否 Special Judge
     */
    private Boolean isSpj;

    private String spjLanguage;

    private String spjCode;

    /**
     * 评测用例（按 seq 升序）
     */
    private List<JudgeCaseDTO> cases;
}
