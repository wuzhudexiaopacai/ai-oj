package com.github.wuzhudexiaopacai.oj.judge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 提交详情 VO（含用例明细）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "提交详情（含用例明细）")
public class SubmissionDetailVO implements Serializable {

    @Schema(description = "提交记录主键 ID（雪花 ID）")
    private Long id;

    @Schema(description = "幂等提交标识（客户端生成的 UUID）")
    private String submitId;

    @Schema(description = "提交用户 ID")
    private Long userId;

    @Schema(description = "题目 ID")
    private Long problemId;

    @Schema(description = "比赛 ID，0 表示非比赛提交")
    private Long contestId;

    @Schema(description = "编程语言：cpp、c、java、python3、go、javascript")
    private String language;

    @Schema(description = "提交的源代码；仅提交本人可查看")
    private String code;

    @Schema(description = "代码长度（字符数）")
    private Integer codeLength;

    @Schema(description = "判题状态码：0 排队中、1 编译中、2 评测中、3 答案正确、4 答案错误、5 运行超时、6 内存超限、7 运行错误、8 编译错误、9 格式错误、10 输出超限、11 系统错误")
    private Integer status;

    @Schema(description = "判题状态中文描述（由状态码映射）")
    private String statusDesc;

    @Schema(description = "得分（各用例得分之和；排队/评测中为 0）")
    private Integer score;

    @Schema(description = "最大耗时（毫秒），取所有用例耗时的最大值；未产出时为 0")
    private Integer timeUsed;

    @Schema(description = "最大内存（KB），取所有用例内存的最大值；未产出时为 0")
    private Integer memoryUsed;

    @Schema(description = "编译/运行错误信息；无错误时为 null")
    private String errorMessage;

    @Schema(description = "判题完成时间，格式 yyyy-MM-dd HH:mm:ss；未完成时为 null")
    private LocalDateTime judgeTime;

    @Schema(description = "提交创建时间，格式 yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 判题用例明细
     */
    @Schema(description = "判题用例明细，按 seq 升序；排队/编译阶段可能为空列表")
    private List<CaseResultItem> cases;

    /**
     * 单用例结果
     */
    @Data
    @Schema(description = "单用例判题结果")
    public static class CaseResultItem implements Serializable {

        @Schema(description = "用例序号（从 1 开始，与题目用例的 seq 对应）")
        private Integer seq;

        @Schema(description = "该用例状态码：3 答案正确、4 答案错误、5 运行超时、6 内存超限、7 运行错误、8 编译错误、9 格式错误、10 输出超限、11 系统错误")
        private Integer status;

        @Schema(description = "该用例状态中文描述（由状态码映射）")
        private String statusDesc;

        @Schema(description = "该用例耗时（毫秒）；未产出时为 0")
        private Integer timeUsed;

        @Schema(description = "该用例内存（KB）；未产出时为 0")
        private Integer memoryUsed;

        @Schema(description = "该用例得分")
        private Integer score;

        @Schema(description = "实际输出预览（最多 1024 字符）；无输出时为 null")
        private String outputPreview;

        /**
         * 失败用例的输入（仅非比赛提交返回，超长截断）
         */
        @Schema(description = "失败用例的输入预览；仅普通题目（非比赛）提交返回，超过 4096 字符截断，其余情况为 null")
        private String inputPreview;

        /**
         * 失败用例的期望输出（仅非比赛提交返回，超长截断）
         */
        @Schema(description = "失败用例的期望输出预览；仅普通题目（非比赛）提交返回，超过 4096 字符截断，其余情况为 null")
        private String expectedPreview;
    }
}
