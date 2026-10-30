package com.github.wuzhudexiaopacai.oj.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * AI 生成的评测用例
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "AI 生成的评测用例")
public class GeneratedCaseVO implements Serializable {

    /**
     * 标准输入内容
     */
    @Schema(description = "标准输入内容（多行以 \\n 分隔）", example = "5\n1 2 3 4 5")
    private String inputData;

    /**
     * 期望输出内容
     */
    @Schema(description = "期望输出内容（多行以 \\n 分隔）", example = "15")
    private String outputData;
}
