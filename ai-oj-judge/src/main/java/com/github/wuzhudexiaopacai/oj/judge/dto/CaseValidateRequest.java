package com.github.wuzhudexiaopacai.oj.judge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 用例校验请求：用参考代码逐组实跑，得到真实输出以校验/回填测试用例期望
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "用例校验请求（AI 出题后用参考代码实跑校验测试用例）")
public class CaseValidateRequest {

    @NotBlank(message = "language 不能为空")
    @Schema(description = "参考代码语言，出题校验固定 cpp", example = "cpp",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String language;

    @NotBlank(message = "code 不能为空")
    @Schema(description = "参考代码（标准答案），对每组用例输入实跑得到真实输出",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "单组运行时间限制（毫秒），为空按 10000", example = "2000")
    private Integer timeLimitMs;

    @NotEmpty(message = "cases 不能为空")
    @Valid
    @Schema(description = "待校验用例列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<CaseItem> cases;

    /**
     * 单个用例
     */
    @Data
    @Schema(description = "待校验用例")
    public static class CaseItem {

        @Schema(description = "用例序号，从 1 开始", example = "1")
        private Integer seq;

        @Schema(description = "标准输入内容，可为空（无输入题）")
        private String input;

        @Schema(description = "期望输出（AI 生成值），与参考代码实跑输出比对")
        private String expectedOutput;
    }
}
