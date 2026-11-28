package com.github.wuzhudexiaopacai.oj.file.mq;

import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 文件清理死信消费者
 * <p>
 * 延时清理消息重试超限后进入死信，说明临时分片可能残留：
 * 以 ERROR 日志归档（含 msgId 与 fileId），便于人工清理 MinIO 残留对象。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "%DLQ%" + MqConstants.GROUP_FILE_CLEAN,
        consumerGroup = MqConstants.GROUP_FILE_CLEAN_DLQ)
public class FileCleanDlqConsumer implements RocketMQListener<MessageExt> {

    @Override
    public void onMessage(MessageExt message) {
        log.error("文件清理消息进入死信，临时分片可能残留: msgId={}, retryTimes={}, body={}",
                message.getMsgId(), message.getReconsumeTimes(),
                new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
