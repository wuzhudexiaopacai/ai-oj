package com.github.wuzhudexiaopacai.oj.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 评测用例（判题服务向题目服务拉取）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class JudgeCaseDTO implements Serializable {

    private Long caseId;

    private Integer seq;

    /**
     * 输入内容（小数据内联；大数据为空，走 inputFile 从 MinIO 拉取）
     */
    private String input;

    /**
     * 期望输出内容
     */
    private String output;

    /**
     * MinIO 输入文件 key
     */
    private String inputFile;

    /**
     * MinIO 输出文件 key
     */
    private String outputFile;

    private Integer score;

    private String groupName;
}
