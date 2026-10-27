package com.github.wuzhudexiaopacai.oj.problem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 题目列表项
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "题目列表项")
public class ProblemPageVO implements Serializable {

    @Schema(description = "题目 ID")
    private Long id;

    @Schema(description = "展示题号，如 A1001")
    private String problemKey;

    @Schema(description = "题目标题")
    private String title;

    /**
     * 难度：1简单 2中等 3困难
     */
    @Schema(description = "难度：0简单 1中等 2困难")
    private Integer difficulty;

    /**
     * 标签列表
     */
    @Schema(description = "标签列表，无标签时为空列表")
    private List<TagVO> tags;

    @Schema(description = "通过次数（MQ 异步统计，可能为 0）")
    private Integer acceptCount;

    @Schema(description = "提交次数（MQ 异步统计，可能为 0）")
    private Integer submitCount;

    /**
     * 通过率（百分比，submitCount=0 时为 0）
     */
    @Schema(description = "通过率，百分比数值，保留两位小数；submitCount 为 0 时为 0")
    private BigDecimal passRate;

    /**
     * 当前用户完成状态（本接口保持无状态，恒为 null；
     * 登录用户的通过状态由调用方走 submission 服务接口获取后自行合并）
     */
    @Schema(description = "当前用户完成状态：本接口保持无状态，恒为 null，登录用户的通过状态由调用方合并")
    private Integer userStatus;
}
