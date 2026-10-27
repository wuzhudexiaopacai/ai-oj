package com.github.wuzhudexiaopacai.oj.problem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 样例用例展示对象（大数据文件用例不返回内容，仅返回标志）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "样例用例展示对象")
public class SampleCaseVO implements Serializable {

    @Schema(description = "样例序号，题目内唯一")
    private Integer seq;

    /**
     * 输入内容（大数据用例为 null）
     */
    @Schema(description = "样例输入内容；对应大数据文件时（inputBig=true）为 null")
    private String input;

    /**
     * 输出内容（大数据用例为 null）
     */
    @Schema(description = "样例输出内容；对应大数据文件时（outputBig=true）为 null")
    private String output;

    /**
     * 输入是否为大数据文件（true 时内容不返回）
     */
    @Schema(description = "输入是否为 MinIO 大数据文件，为 true 时 input 不返回内容")
    private Boolean inputBig;

    /**
     * 输出是否为大数据文件
     */
    @Schema(description = "输出是否为 MinIO 大数据文件，为 true 时 output 不返回内容")
    private Boolean outputBig;
}
