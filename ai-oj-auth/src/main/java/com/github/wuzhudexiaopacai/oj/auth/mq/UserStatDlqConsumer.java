package com.github.wuzhudexiaopacai.oj.auth.mq;

import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 用户统计死信消费者
 * <p>
 * auth 侧无死信表，重试超限的消息以 ERROR 日志归档（含 msgId 与原始消息体），
 * 可在 Kibana 检索；需要补偿时在 RocketMQ 控制台按 msgId 重放。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "%DLQ%" + MqConstants.GROUP_USER_STAT,
        consumerGroup = MqConstants.GROUP_USER_STAT_DLQ)
public class UserStatDlqConsumer implements RocketMQListener<MessageExt> {

    @Override
    public void onMessage(MessageExt message) {
        log.error("用户统计消息进入死信，已归档待人工处理: msgId={}, retryTimes={}, body={}",
                message.getMsgId(), message.getReconsumeTimes(),
                new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
