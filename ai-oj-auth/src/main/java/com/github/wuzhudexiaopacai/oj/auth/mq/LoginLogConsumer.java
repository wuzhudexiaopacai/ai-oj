package com.github.wuzhudexiaopacai.oj.auth.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysLoginLog;
import com.github.wuzhudexiaopacai.oj.auth.mapper.SysLoginLogMapper;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 登录日志消费者（异步落库 sys_login_log）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = MqConstants.LOGIN_LOG_TOPIC, consumerGroup = MqConstants.GROUP_LOGIN_LOG)
public class LoginLogConsumer implements RocketMQListener<String> {

    private final SysLoginLogMapper loginLogMapper;

    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(String message) {
        try {
            SysLoginLog loginLog = objectMapper.readValue(message, SysLoginLog.class);
            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            // 消息体损坏不重试，直接丢弃避免毒消息打满重试队列
            log.error("登录日志消息解析失败，已丢弃：{}，原因：{}", message, e.getMessage());
        }
    }
}
