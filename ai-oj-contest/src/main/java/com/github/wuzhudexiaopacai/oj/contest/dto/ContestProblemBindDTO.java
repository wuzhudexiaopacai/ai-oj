package com.github.wuzhudexiaopacai.oj.contest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 比赛题目绑定入参（display_id 由服务端按已有题目数量自动生成 A/B/C...）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "比赛题目绑定入参，display_id 由服务端按已有题目数量自动生成 A/B/C...")
public class ContestProblemBindDTO implements Serializable {

    /**
     * 题目 ID
     */
    @Schema(description = "题目 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "题目ID不能为空")
    private Long problemId;

    /**
     * 赛中展示标题（可覆盖原题，空则展示原题标题）
     */
    @Schema(description = "赛中展示标题，可覆盖原题标题，最长 128 字符；留空则展示原题标题", example = "A+B Problem")
    @Size(max = 128, message = "展示标题长度不能超过128")
    private String title;

    /**
     * OI 模式分值
     */
    @Schema(description = "OI 赛制下的题目分值，ACM 赛制忽略；留空默认 0", example = "100")
    private Integer score;
}
