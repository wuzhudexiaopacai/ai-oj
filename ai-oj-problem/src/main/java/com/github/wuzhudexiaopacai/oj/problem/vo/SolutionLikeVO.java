package com.github.wuzhudexiaopacai.oj.problem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 题解点赞结果
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "题解点赞结果")
public class SolutionLikeVO implements Serializable {

    @Schema(description = "当前用户点赞后是否为已点赞状态", example = "true")
    private Boolean liked;

    @Schema(description = "当前点赞总数", example = "12")
    private Integer likeCount;
}
