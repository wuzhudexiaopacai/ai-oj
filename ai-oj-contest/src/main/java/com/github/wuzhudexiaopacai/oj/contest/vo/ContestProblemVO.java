package com.github.wuzhudexiaopacai.oj.contest.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 比赛题目展示对象
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "比赛题目展示对象")
public class ContestProblemVO implements Serializable {

    /**
     * 题目 ID
     */
    @Schema(description = "题目 ID", example = "1001")
    private Long problemId;

    /**
     * 赛中题号，如 A
     */
    @Schema(description = "赛中题号，按绑定顺序自动生成，如 A、B（最多 26 题）", example = "A")
    private String displayId;

    /**
     * 赛中展示标题（空则前端回退展示原题标题）
     */
    @Schema(description = "赛中展示标题；为空时前端回退展示原题标题", example = "A+B Problem")
    private String title;

    /**
     * OI 模式分值
     */
    @Schema(description = "OI 赛制下的题目分值；ACM 赛制下无意义，未填时为 0", example = "100")
    private Integer score;

    /**
     * 排序
     */
    @Schema(description = "排序序号，从 0 开始，对应题目在比赛中的展示顺序", example = "0")
    private Integer sort;

    /**
     * 当前用户在该比赛内的做题状态：AC=已通过，ATTEMPTED=尝试过，为空=未提交
     * <p>
     * 按 contestId 独立计算，不含赛前历史提交；未登录时为空。
     */
    @Schema(description = "当前用户在该比赛内的做题状态：AC 已通过 / ATTEMPTED 尝试过 / null 未提交；按比赛独立计算、不含赛前历史提交，未登录时为 null", example = "AC")
    private String status;
}
