package com.github.wuzhudexiaopacai.oj.contest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.contest.vo.ContestTimeVO;
import com.github.wuzhudexiaopacai.oj.contest.websocket.ContestWsPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis 订阅配置：比赛时间变更的跨实例 WebSocket 推送路由
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ContestWsRedisConfig {

    private final ContestWsPushService wsPushService;

    private final ObjectMapper objectMapper;

    /**
     * 订阅 "contest:ws:push" 频道，收到后转发本机该比赛的全部 WebSocket 会话
     */
    @Bean
    public RedisMessageListenerContainer contestWsListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            try {
                ContestTimeVO timeVO = objectMapper.readValue(message.getBody(), ContestTimeVO.class);
                wsPushService.pushLocal(timeVO);
            } catch (Exception e) {
                log.error("比赛时间推送消息处理失败", e);
            }
        }, new ChannelTopic(ContestWsPushService.WS_PUSH_CHANNEL));
        return container;
    }
}
