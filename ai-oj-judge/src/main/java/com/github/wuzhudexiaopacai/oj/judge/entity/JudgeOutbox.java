package com.github.wuzhudexiaopacai.oj.judge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 判题结果本地消息表（Outbox）
 * <p>
 * 判题终态落库后先写本表再投递 MQ，投递失败由补偿任务按退避重投，
 * 避免 broadcast 异常导致题目统计/比赛成绩永久丢失该次结果。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("judge_outbox")
public class JudgeOutbox extends BaseEntity {

    /**
     * 待投递
     */
    public static final int STATUS_PENDING = 0;

    /**
     * 已投递
     */
    public static final int STATUS_SENT = 1;

    /**
     * 提交业务 ID（幂等键）
     */
    private String submitId;

    /**
     * 待投递的结果消息 JSON
     */
    private String payload;

    /**
     * 投递状态：0 待投递 1 已投递
     */
    private Integer status;

    /**
     * 已重试次数
     */
    private Integer retryCount;

    /**
     * 下次重试时间（退避）
     */
    private LocalDateTime nextRetryTime;
}
