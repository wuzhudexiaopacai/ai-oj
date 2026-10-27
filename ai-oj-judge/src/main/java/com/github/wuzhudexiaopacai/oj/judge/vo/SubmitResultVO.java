package com.github.wuzhudexiaopacai.oj.judge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 提交结果
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@AllArgsConstructor
@Schema(description = "提交结果")
public class SubmitResultVO {

    /**
     * 提交记录 ID
     */
    @Schema(description = "提交记录主键 ID（雪花 ID），可用于查询提交详情")
    private Long submissionId;
}
