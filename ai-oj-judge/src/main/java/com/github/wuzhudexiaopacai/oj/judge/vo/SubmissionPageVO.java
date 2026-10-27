package com.github.wuzhudexiaopacai.oj.judge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提交分页 VO
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "提交分页记录")
public class SubmissionPageVO implements Serializable {

    @Schema(description = "提交记录主键 ID（雪花 ID）")
    private Long id;

    @Schema(description = "幂等提交标识（客户端生成的 UUID）")
    private String submitId;

    @Schema(description = "题目 ID")
    private Long problemId;

    /**
     * 展示题号（由题目服务回填）
     */
    @Schema(description = "展示题号；由题目服务回填，回填失败时为 null")
    private String problemKey;

    /**
     * 题目标题（由题目服务回填）
     */
    @Schema(description = "题目标题；由题目服务回填，回填失败时为 null")
    private String problemTitle;

    @Schema(description = "比赛 ID，0 表示非比赛提交")
    private Long contestId;

    @Schema(description = "编程语言：cpp、c、java、python3、go、javascript")
    private String language;

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

    @Schema(description = "判题完成时间，格式 yyyy-MM-dd HH:mm:ss；未完成时为 null")
    private LocalDateTime judgeTime;

    @Schema(description = "提交创建时间，格式 yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
