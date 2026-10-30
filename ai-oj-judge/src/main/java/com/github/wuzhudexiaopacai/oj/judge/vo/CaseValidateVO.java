package com.github.wuzhudexiaopacai.oj.judge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用例校验结果：逐组返回参考代码实跑的真实输出、是否与期望一致
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "用例校验结果")
public class CaseValidateVO {

    @Schema(description = "参考代码是否编译通过")
    private Boolean compileOk;

    @Schema(description = "编译失败日志（compileOk=false 时有值）")
    private String compileLog;

    @Schema(description = "是否全部用例校验通过（编译成功且每组实际输出与期望一致）")
    private Boolean overallPassed;

    @Schema(description = "用例总数")
    private Integer totalCount;

    @Schema(description = "校验通过数")
    private Integer passedCount;

    @Schema(description = "逐组校验明细")
    private List<ItemResult> cases;

    /**
     * 单组校验结果
     */
    @Data
    @Schema(description = "单组校验结果")
    public static class ItemResult {

        @Schema(description = "用例序号")
        private Integer seq;

        @Schema(description = "是否通过（实际输出与期望归一化后一致）")
        private Boolean passed;

        @Schema(description = "参考代码实跑的真实输出（可用于回填期望）")
        private String actualOutput;

        @Schema(description = "原期望输出（AI 生成值）")
        private String expectedOutput;

        @Schema(description = "运行状态：ACCEPTED/WRONG_ANSWER/RUNTIME_ERROR/COMPILE_ERROR/TIME_LIMIT_EXCEEDED/SYSTEM_ERROR")
        private String status;

        @Schema(description = "错误信息（运行异常时有值）")
        private String errorMessage;

        @Schema(description = "耗时（毫秒）")
        private Integer timeMs;

        @Schema(description = "内存（KB）")
        private Integer memoryKb;
    }
}
