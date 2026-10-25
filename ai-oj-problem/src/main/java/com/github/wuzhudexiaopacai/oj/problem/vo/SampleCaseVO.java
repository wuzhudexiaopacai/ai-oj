package com.github.wuzhudexiaopacai.oj.problem.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 样例用例展示对象（大数据文件用例不返回内容，仅返回标志）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class SampleCaseVO implements Serializable {

    private Integer seq;

    /**
     * 输入内容（大数据用例为 null）
     */
    private String input;

    /**
     * 输出内容（大数据用例为 null）
     */
    private String output;

    /**
     * 输入是否为大数据文件（true 时内容不返回）
     */
    private Boolean inputBig;

    /**
     * 输出是否为大数据文件
     */
    private Boolean outputBig;
}
