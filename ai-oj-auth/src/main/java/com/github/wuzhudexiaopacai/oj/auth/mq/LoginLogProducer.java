package com.github.wuzhudexiaopacai.oj.auth.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.auth.entity.SysLoginLog;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 登录日志生产者（异步发送，不阻塞登录主链路）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginLogProducer {

    private final RocketMQTemplate rocketMQTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 发送登录日志
     *
     * @param userId    用户ID（失败可为空）
     * @param username  登录名
     * @param ip        登录IP
     * @param userAgent UA
     * @param success   是否成功：1成功 0失败
     * @param message   附加信息
     */
    public void send(Long userId, String username, String ip, String userAgent, Integer success, String message) {
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUserId(userId);
        loginLog.setUsername(username == null ? "" : username);
        loginLog.setIp(ip == null ? "" : ip);
        loginLog.setUserAgent(userAgent == null ? "" : userAgent);
        loginLog.setSuccess(success);
        loginLog.setMessage(message == null ? "" : message);
        loginLog.setLoginTime(LocalDateTime.now());
        try {
            rocketMQTemplate.asyncSend(MqConstants.LOGIN_LOG_TOPIC, objectMapper.writeValueAsString(loginLog),
                    new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            log.debug("登录日志发送成功，msgId={}", sendResult.getMsgId());
                        }

                        @Override
                        public void onException(Throwable throwable) {
                            log.warn("登录日志发送失败：{}", throwable.getMessage());
                        }
                    });
        } catch (Exception e) {
            // 日志链路异常不影响登录主流程
            log.warn("登录日志序列化/发送异常：{}", e.getMessage());
        }
    }
}
