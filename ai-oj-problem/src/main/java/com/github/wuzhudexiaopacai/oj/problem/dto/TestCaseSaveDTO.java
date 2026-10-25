package com.github.wuzhudexiaopacai.oj.problem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 评测用例保存入参（小数据直接传内容；大数据由前端先传文件服务，再提交 objectKey）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class TestCaseSaveDTO implements Serializable {

    /**
     * 用例 ID（更新时必填，新增为空）
     */
    private Long id;

    /**
     * 用例序号
     */
    @NotNull(message = "用例序号不能为空")
    private Integer seq;

    /**
     * 输入内容（小数据）
     */
    private String inputData;

    /**
     * 期望输出内容（小数据）
     */
    private String outputData;

    /**
     * MinIO 输入文件 objectKey（大数据）
     */
    private String inputFile;

    /**
     * MinIO 输出文件 objectKey（大数据）
     */
    private String outputFile;

    /**
     * OI 模式分值
     */
    private Integer score;
}
