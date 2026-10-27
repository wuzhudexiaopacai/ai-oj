package com.github.wuzhudexiaopacai.oj.problem.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 题目导入/导出 Excel 行模型（导入按 problemKey 判重 upsert）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "题目导入/导出 Excel 行模型（导入按题号判重 upsert）")
public class ProblemImportRow implements Serializable {

    @ExcelProperty("题号")
    @Schema(description = "题号（Excel 列「题号」），导入判重键，必填且不能为空", example = "A1001")
    private String problemKey;

    @ExcelProperty("标题")
    @Schema(description = "题目标题（Excel 列「标题」），必填且不能为空", example = "两数之和")
    private String title;

    @ExcelProperty("描述")
    @Schema(description = "题目描述（Excel 列「描述」），Markdown 文本，必填且不能为空",
            example = "给定两个整数，输出它们的和。")
    private String description;

    @ExcelProperty("输入说明")
    @Schema(description = "输入说明（Excel 列「输入说明」），可空", example = "一行两个整数 a 和 b")
    private String inputDesc;

    @ExcelProperty("输出说明")
    @Schema(description = "输出说明（Excel 列「输出说明」），可空", example = "输出 a + b 的结果")
    private String outputDesc;

    @ExcelProperty("提示")
    @Schema(description = "提示信息（Excel 列「提示」），可空", example = "-10^9 <= a, b <= 10^9")
    private String hint;

    @ExcelProperty("来源")
    @Schema(description = "题目来源（Excel 列「来源」），可空", example = "LeetCode")
    private String source;

    @ExcelProperty("难度(1简单2中等3困难)")
    @Schema(description = "难度（Excel 列「难度(1简单2中等3困难)」），必填，实际取值为 0简单 1中等 2困难，"
            + "后端校验范围 0~2", example = "1")
    private Integer difficulty;

    @ExcelProperty("时间限制(ms)")
    @Schema(description = "时间限制（Excel 列「时间限制(ms)」），单位毫秒（ms），不填默认 1000", example = "1000")
    private Integer timeLimit;

    @ExcelProperty("内存限制(MB)")
    @Schema(description = "内存限制（Excel 列「内存限制(MB)」），单位 MB，不填默认 256", example = "256")
    private Integer memoryLimit;
}
