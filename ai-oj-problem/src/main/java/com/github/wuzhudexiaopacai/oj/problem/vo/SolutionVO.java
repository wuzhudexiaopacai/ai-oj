package com.github.wuzhudexiaopacai.oj.problem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题解展示对象
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "题解展示对象")
public class SolutionVO implements Serializable {

    @Schema(description = "题解 ID")
    private Long id;

    @Schema(description = "所属题目 ID")
    private Long problemId;

    @Schema(description = "作者用户 ID")
    private Long userId;

    @Schema(description = "作者昵称，作者信息不可用时为 null", example = "Alice")
    private String authorName;

    @Schema(description = "题解标题")
    private String title;

    /**
     * 内容（列表接口不返回，详情返回）
     */
    @Schema(description = "题解正文（Markdown）；列表接口不返回，为 null")
    private String content;

    @Schema(description = "点赞数，初始为 0")
    private Integer likeCount;

    @Schema(description = "当前登录用户是否已点赞；未登录固定为 false", example = "false")
    private Boolean liked;

    @Schema(description = "创建时间，格式 yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
