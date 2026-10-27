package com.github.wuzhudexiaopacai.oj.problem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "评测用例保存入参")
public class TestCaseSaveDTO implements Serializable {

    /**
     * 用例 ID（更新时必填，新增为空）
     */
    @Schema(description = "用例 ID，新增时为空，更新时由路径参数指定", example = "30001")
    private Long id;

    /**
     * 用例序号
     */
    @NotNull(message = "用例序号不能为空")
    @Schema(description = "用例序号，题目内用于排序；必填", example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer seq;

    /**
     * 输入内容（小数据）
     */
    @Schema(description = "输入内容（小数据场景），与 inputFile 二选一；两者至少填一个", example = "1 2")
    private String inputData;

    /**
     * 期望输出内容（小数据）
     */
    @Schema(description = "期望输出内容（小数据场景），与 outputFile 二选一；两者至少填一个", example = "3")
    private String outputData;

    /**
     * MinIO 输入文件 objectKey（大数据）
     */
    @Schema(description = "MinIO 输入文件 objectKey（大数据场景），需先调用文件服务上传获得；与 inputData 二选一",
            example = "testcase/10001/1.in")
    private String inputFile;

    /**
     * MinIO 输出文件 objectKey（大数据）
     */
    @Schema(description = "MinIO 输出文件 objectKey（大数据场景），需先调用文件服务上传获得；与 outputData 二选一",
            example = "testcase/10001/1.out")
    private String outputFile;

    /**
     * OI 模式分值
     */
    @Schema(description = "OI 计分模式下该用例的分值，ACM 模式可忽略", example = "10")
    private Integer score;
}
