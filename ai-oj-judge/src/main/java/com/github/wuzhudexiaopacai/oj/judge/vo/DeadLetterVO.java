package com.github.wuzhudexiaopacai.oj.judge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 死信归档视图
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "死信归档记录")
public class DeadLetterVO {

    @Schema(description = "记录 ID（雪花 ID）", example = "1960000000000000001")
    private Long id;

    @Schema(description = "原 Topic", example = "AI_OJ_JUDGE_SUBMIT")
    private String topic;

    @Schema(description = "原消费组", example = "ai-oj-judge-submit-consumer")
    private String consumerGroup;

    @Schema(description = "RocketMQ 消息 ID", example = "AC16000F00072D98A3352412739E0002")
    private String msgId;

    @Schema(description = "消息体（原始 JSON）")
    private String msgBody;

    @Schema(description = "进入死信前已重试次数", example = "16")
    private Integer retryTimes;

    @Schema(description = "状态：0 待处理、1 已重放、2 已忽略", example = "0")
    private Integer status;

    @Schema(description = "状态文案", example = "待处理")
    private String statusLabel;

    @Schema(description = "处理备注", example = "沙箱恢复后重放成功")
    private String handleRemark;

    @Schema(description = "归档时间", example = "2026-11-08T10:30:00")
    private LocalDateTime createTime;
}
