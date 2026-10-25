package com.github.wuzhudexiaopacai.oj.judge.sandbox.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * 沙箱判题响应（契约字段为下划线命名）
 *
 * @param compile       编译结果
 * @param overallStatus 整体状态字符串（AC/WA/TLE/MLE/RE/CE/PE/OLE/SYSTEM_ERROR）
 * @param totalScore    总分
 * @param cases         单用例结果
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SandboxJudgeResponse(CompileResult compile,
                                   String overallStatus,
                                   Integer totalScore,
                                   List<CaseResult> cases) {

    /**
     * 编译结果
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CompileResult(Boolean success, String log) {
    }

    /**
     * 单用例结果
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CaseResult(Integer seq,
                             String status,
                             Integer timeMs,
                             Integer memoryKb,
                             Integer score,
                             String outputPreview) {
    }
}
