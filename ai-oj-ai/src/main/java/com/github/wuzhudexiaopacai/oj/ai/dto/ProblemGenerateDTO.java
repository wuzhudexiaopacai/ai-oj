package com.github.wuzhudexiaopacai.oj.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * AI 出题请求
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "AI 出题请求")
public class ProblemGenerateDTO implements Serializable {

    /**
     * 知识点
     */
    @NotBlank(message = "知识点不能为空")
    @Schema(description = "知识点/主题，如 动态规划、图论最短路", example = "动态规划",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String topic;

    /**
     * 难度：0 简单 / 1 中等 / 2 困难
     */
    @Min(value = 0, message = "难度取值为 0/1/2")
    @Max(value = 2, message = "难度取值为 0/1/2")
    @Schema(description = "难度：0 简单 / 1 中等 / 2 困难，默认 1", example = "1")
    private Integer difficulty = 1;

    /**
     * 管理员自定义出题要求（可选）
     */
    @Schema(description = "额外的出题要求/约束（可选），如 结合生活场景、数据范围 1e5、需要多组输入",
            example = "结合背包场景，数据范围不超过 1e3")
    private String requirement;

    /**
     * 期望生成的测试用例组数
     */
    @Min(value = 1, message = "测试用例至少 1 组")
    @Max(value = 20, message = "测试用例最多 20 组")
    @Schema(description = "期望生成的测试用例组数，默认 10", example = "10")
    private Integer caseCount = 10;
}
