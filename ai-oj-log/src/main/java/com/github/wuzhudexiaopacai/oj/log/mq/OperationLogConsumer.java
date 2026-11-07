package com.github.wuzhudexiaopacai.oj.log.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.dto.OperationLogMessage;
import com.github.wuzhudexiaopacai.oj.log.entity.SysOperationLog;
import com.github.wuzhudexiaopacai.oj.log.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * 操作日志消费者：接收各业务服务投递的日志消息并统一落库
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqConstants.OPERATION_LOG_TOPIC,
        consumerGroup = MqConstants.GROUP_OPERATION_LOG
)
public class OperationLogConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;

    private final SysOperationLogMapper operationLogMapper;

    @Override
    public void onMessage(String message) {
        try {
            OperationLogMessage source = objectMapper.readValue(message, OperationLogMessage.class);
            SysOperationLog entity = new SysOperationLog();
            BeanUtils.copyProperties(source, entity);
            // 消费线程没有登录上下文，审计人直接用消息里的操作人，避免 create_by/update_by 恒为 NULL
            entity.setCreateBy(source.getUserId());
            entity.setUpdateBy(source.getUserId());
            operationLogMapper.insert(entity);
        } catch (Exception e) {
            // 消费失败不重试爆队列，记录到运行日志由 Kibana 检索
            log.error("操作日志消费失败：{}", e.getMessage(), e);
        }
    }
}
