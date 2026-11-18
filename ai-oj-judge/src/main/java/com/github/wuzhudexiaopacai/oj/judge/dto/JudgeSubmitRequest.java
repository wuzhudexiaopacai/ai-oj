package com.github.wuzhudexiaopacai.oj.judge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 判题提交请求
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "判题提交请求")
public class JudgeSubmitRequest {

    /**
     * 幂等提交标识（客户端生成）
     */
    @NotBlank(message = "submitId 不能为空")
    @Size(max = 64, message = "submitId 长度不能超过 64")
    @Schema(description = "客户端生成的幂等提交标识（UUID，不能为空）；同一 submitId 重复提交直接返回已有提交记录",
            example = "3f2b8c1e-9a4d-4e6b-8c2f-1d2e3a4b5c6d",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String submitId;

    @NotNull(message = "problemId 不能为空")
    @Positive(message = "题目 ID 必须为正数")
    @Schema(description = "题目 ID，不能为空", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long problemId;

    @NotBlank(message = "language 不能为空")
    @Size(max = 32, message = "语言标识长度不能超过 32")
    @Schema(description = "编程语言，不能为空：cpp/C++17、c/C11、java/Java17、python3/Python3、go、javascript",
            example = "cpp", requiredMode = Schema.RequiredMode.REQUIRED)
    private String language;

    @NotBlank(message = "code 不能为空")
    @Size(max = 65535, message = "代码长度不能超过 65535 个字符")
    @Schema(description = "提交的源代码，不能为空，最长 65535 个字符；长度会记录到提交记录的 codeLength",
            example = "#include <iostream>\nint main() { return 0; }",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    /**
     * 比赛 ID（非比赛提交不传，默认 0）
     */
    @PositiveOrZero(message = "比赛 ID 不能为负数")
    @Schema(description = "比赛 ID；非比赛提交不传或传 0。传入非 0 时校验已报名、比赛进行中且题目属于该比赛",
            example = "0")
    private Long contestId;
}
