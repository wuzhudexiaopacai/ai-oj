package com.github.wuzhudexiaopacai.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评测用例实体（pms_test_case），大文件走 MinIO（input_file/output_file）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_test_case")
@Schema(description = "评测用例实体（管理端用例列表直接返回该实体）")
public class TestCase extends BaseEntity {

    /**
     * 题目 ID
     */
    @Schema(description = "所属题目 ID（雪花 ID）", example = "10001")
    private Long problemId;

    /**
     * 用例序号
     */
    @Schema(description = "用例序号，从 1 开始，决定评测执行顺序", example = "1")
    private Integer seq;

    /**
     * 输入（小数据直接存库）
     */
    @Schema(description = "输入数据，小数据直接存库；大数据为空并使用 inputFile", example = "1 2")
    private String inputData;

    /**
     * 期望输出（小数据直接存库）
     */
    @Schema(description = "期望输出，小数据直接存库；大数据为空并使用 outputFile", example = "3")
    private String outputData;

    /**
     * MinIO 输入文件对象名（大数据）
     */
    @Schema(description = "MinIO 输入文件对象名，仅大数据用例有值", example = "files/9e107d9d372bb6826bd81d3542a419d6/case.in")
    private String inputFile;

    /**
     * MinIO 输出文件对象名（大数据）
     */
    @Schema(description = "MinIO 输出文件对象名，仅大数据用例有值", example = "files/9e107d9d372bb6826bd81d3542a419d6/case.out")
    private String outputFile;

    /**
     * OI 模式该用例分值
     */
    @Schema(description = "OI 模式该用例分值，ACM 模式不使用", example = "10")
    private Integer score;
}
