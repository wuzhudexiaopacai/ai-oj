package com.github.wuzhudexiaopacai.oj.judge.sandbox.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * 沙箱判题请求（契约字段为下划线命名）
 *
 * @param submissionId  提交记录 ID
 * @param language      编程语言
 * @param code          提交代码
 * @param timeLimitMs   时间限制（毫秒）
 * @param memoryLimitMb 内存限制（MB）
 * @param stackLimitMb  栈空间限制（MB）
 * @param ioScore       是否 OI 计分（按用例分值累加；false 为 ACM 全对计分）
 * @param cases         评测用例
 * @param spj           Special Judge（无则为 null）
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SandboxJudgeRequest(Long submissionId,
                                  String language,
                                  String code,
                                  Integer timeLimitMs,
                                  Integer memoryLimitMb,
                                  Integer stackLimitMb,
                                  Boolean ioScore,
                                  List<CaseItem> cases,
                                  SpjItem spj) {

    /**
     * 评测用例
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CaseItem(Integer seq,
                           String input,
                           String expectedOutput,
                           Integer score) {
    }

    /**
     * Special Judge 程序
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SpjItem(String language, String code) {
    }
}
