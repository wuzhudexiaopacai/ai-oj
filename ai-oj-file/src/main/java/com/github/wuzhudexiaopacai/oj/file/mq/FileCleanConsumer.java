package com.github.wuzhudexiaopacai.oj.file.mq;

import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.file.dto.FileCleanMessage;
import com.github.wuzhudexiaopacai.oj.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 文件清理消费者（延时消息兜底清理超时未完成的分片上传）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = MqConstants.FILE_CLEAN_TOPIC, consumerGroup = MqConstants.GROUP_FILE_CLEAN)
public class FileCleanConsumer implements RocketMQListener<FileCleanMessage> {

    /**
     * 清理幂等标记前缀（+ fileId，TTL 7 天）
     */
    private static final String CLEAN_IDEM_KEY = "file:clean:idem:";

    private final FileService fileService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(FileCleanMessage message) {
        if (message == null || message.getFileId() == null) {
            log.warn("文件清理消息字段缺失，直接消费成功: {}", message);
            return;
        }
        // 幂等：同一文件只清理一次，重复消息直接消费成功
        String idemKey = CLEAN_IDEM_KEY + message.getFileId();
        Boolean first = redisTemplate.opsForValue().setIfAbsent(idemKey, "1", 7, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(first)) {
            log.info("文件清理消息重复，跳过: fileId={}", message.getFileId());
            return;
        }
        try {
            fileService.cleanUpload(message.getFileId(), message.getMd5());
        } catch (Exception e) {
            // 处理失败删除幂等标记，让消息重试时重新消费
            redisTemplate.delete(idemKey);
            log.error("文件清理失败: {}", message, e);
            throw e;
        }
    }
}
