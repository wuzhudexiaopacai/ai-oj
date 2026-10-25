package com.github.wuzhudexiaopacai.oj.judge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务死信归档
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_dead_letter")
public class BizDeadLetter extends BaseEntity {

    /**
     * 原 Topic
     */
    private String topic;

    /**
     * 原消费组
     */
    private String consumerGroup;

    /**
     * RocketMQ 消息 ID
     */
    private String msgId;

    /**
     * 消息体
     */
    private String msgBody;

    /**
     * 已重试次数
     */
    private Integer retryTimes;

    /**
     * 状态：0 待处理 1 已重放 2 已忽略
     */
    private Integer status;

    /**
     * 处理备注
     */
    private String handleRemark;
}
