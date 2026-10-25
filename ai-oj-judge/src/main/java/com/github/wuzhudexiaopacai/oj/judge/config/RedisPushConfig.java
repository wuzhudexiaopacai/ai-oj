package com.github.wuzhudexiaopacai.oj.judge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.judge.vo.JudgeProgressVO;
import com.github.wuzhudexiaopacai.oj.judge.websocket.JudgeWsPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis 订阅配置：跨实例 WebSocket 推送路由
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisPushConfig {

    private final JudgeWsPushService judgeWsPushService;

    private final ObjectMapper objectMapper;

    /**
     * 订阅 "judge:ws:push" 频道，收到后转发本机 WebSocket 会话
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            try {
                JudgeProgressVO progress = objectMapper.readValue(message.getBody(), JudgeProgressVO.class);
                judgeWsPushService.pushLocal(progress.getSubmitId(), progress);
            } catch (Exception e) {
                log.error("判题结果推送消息处理失败", e);
            }
        }, new ChannelTopic(JudgeWsPushService.WS_PUSH_CHANNEL));
        return container;
    }
}
