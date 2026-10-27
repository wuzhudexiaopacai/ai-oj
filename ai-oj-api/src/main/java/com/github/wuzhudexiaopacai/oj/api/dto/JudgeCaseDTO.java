package com.github.wuzhudexiaopacai.oj.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 评测用例（判题服务向题目服务拉取）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "评测用例（内部 Feign 契约，小数据内联、大数据走 MinIO 对象 key）")
public class JudgeCaseDTO implements Serializable {

    @Schema(description = "用例 ID（雪花 ID）", example = "20001")
    private Long caseId;

    @Schema(description = "用例序号，从 1 开始，决定执行顺序", example = "1")
    private Integer seq;

    /**
     * 输入内容（小数据内联；大数据为空，走 inputFile 从 MinIO 拉取）
     */
    @Schema(description = "输入内容，小数据内联；大数据为空并改走 inputFile", example = "1 2")
    private String input;

    /**
     * 期望输出内容
     */
    @Schema(description = "期望输出内容，小数据内联；大数据为空并改走 outputFile", example = "3")
    private String output;

    /**
     * MinIO 输入文件 key
     */
    @Schema(description = "MinIO 输入文件对象 key，仅大数据用例有值", example = "files/9e107d9d372bb6826bd81d3542a419d6/case.in")
    private String inputFile;

    /**
     * MinIO 输出文件 key
     */
    @Schema(description = "MinIO 输出文件对象 key，仅大数据用例有值", example = "files/9e107d9d372bb6826bd81d3542a419d6/case.out")
    private String outputFile;

    @Schema(description = "OI 模式该用例分值，ACM 模式不使用", example = "10")
    private Integer score;

    @Schema(description = "用例分组名，用于子任务计分，可为空", example = "group1")
    private String groupName;
}
