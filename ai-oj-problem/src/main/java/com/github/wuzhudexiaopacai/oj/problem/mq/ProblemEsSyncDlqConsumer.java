package com.github.wuzhudexiaopacai.oj.problem.mq;

import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 题目 ES 同步死信消费者
 * <p>
 * ES 只是检索索引，DB 为权威数据，订阅到死信说明文档可能滞后：
 * 以 ERROR 日志归档（含 msgId 与题目 ID），后续更新题目或重建索引即可追平。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "%DLQ%" + MqConstants.GROUP_PROBLEM_ES,
        consumerGroup = MqConstants.GROUP_PROBLEM_ES_DLQ)
public class ProblemEsSyncDlqConsumer implements RocketMQListener<MessageExt> {

    @Override
    public void onMessage(MessageExt message) {
        log.error("题目 ES 同步消息进入死信，检索文档可能滞后: msgId={}, retryTimes={}, body={}",
                message.getMsgId(), message.getReconsumeTimes(),
                new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
