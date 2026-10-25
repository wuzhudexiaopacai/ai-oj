package com.github.wuzhudexiaopacai.oj.problem.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 题目导入/导出 Excel 行模型（导入按 problemKey 判重 upsert）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class ProblemImportRow implements Serializable {

    @ExcelProperty("题号")
    private String problemKey;

    @ExcelProperty("标题")
    private String title;

    @ExcelProperty("描述")
    private String description;

    @ExcelProperty("输入说明")
    private String inputDesc;

    @ExcelProperty("输出说明")
    private String outputDesc;

    @ExcelProperty("提示")
    private String hint;

    @ExcelProperty("来源")
    private String source;

    @ExcelProperty("难度(1简单2中等3困难)")
    private Integer difficulty;

    @ExcelProperty("时间限制(ms)")
    private Integer timeLimit;

    @ExcelProperty("内存限制(MB)")
    private Integer memoryLimit;
}
