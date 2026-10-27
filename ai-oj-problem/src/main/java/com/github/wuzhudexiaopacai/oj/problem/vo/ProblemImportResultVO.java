package com.github.wuzhudexiaopacai.oj.problem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 题目导入结果
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "题目导入结果")
public class ProblemImportResultVO implements Serializable {

    /**
     * 成功条数
     */
    @Schema(description = "导入成功条数")
    private int successCount;

    /**
     * 失败条数
     */
    @Schema(description = "导入失败条数")
    private int failCount;

    /**
     * 错误行信息（行号: 错误原因）
     */
    @Schema(description = "错误明细列表，格式为「第N行: 错误原因」；无错误时为空列表")
    private List<String> errors;
}
