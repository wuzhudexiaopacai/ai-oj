package com.github.wuzhudexiaopacai.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
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
public class TestCase extends BaseEntity {

    /**
     * 题目 ID
     */
    private Long problemId;

    /**
     * 用例序号
     */
    private Integer seq;

    /**
     * 输入（小数据直接存库）
     */
    private String inputData;

    /**
     * 期望输出（小数据直接存库）
     */
    private String outputData;

    /**
     * MinIO 输入文件对象名（大数据）
     */
    private String inputFile;

    /**
     * MinIO 输出文件对象名（大数据）
     */
    private String outputFile;

    /**
     * OI 模式该用例分值
     */
    private Integer score;
}
