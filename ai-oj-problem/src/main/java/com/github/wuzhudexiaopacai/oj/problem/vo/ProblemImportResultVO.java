package com.github.wuzhudexiaopacai.oj.problem.vo;

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
public class ProblemImportResultVO implements Serializable {

    /**
     * 成功条数
     */
    private int successCount;

    /**
     * 失败条数
     */
    private int failCount;

    /**
     * 错误行信息（行号: 错误原因）
     */
    private List<String> errors;
}
