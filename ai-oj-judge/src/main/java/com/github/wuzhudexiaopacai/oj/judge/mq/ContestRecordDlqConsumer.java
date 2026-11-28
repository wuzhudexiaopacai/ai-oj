package com.github.wuzhudexiaopacai.oj.judge.mq;

import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 比赛成绩死信消费者
 * <p>
 * 成绩消息重试超限后进入死信，说明某次结果未能合并进 contest_record：
 * 以 ERROR 日志归档（含 msgId 与结果消息体），可在 Kibana 检索并按 msgId 手动重放。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "%DLQ%" + MqConstants.GROUP_CONTEST_RECORD,
        consumerGroup = MqConstants.GROUP_CONTEST_RECORD_DLQ)
public class ContestRecordDlqConsumer implements RocketMQListener<MessageExt> {

    @Override
    public void onMessage(MessageExt message) {
        log.error("比赛成绩消息进入死信，成绩可能未合并: msgId={}, retryTimes={}, body={}",
                message.getMsgId(), message.getReconsumeTimes(),
                new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
