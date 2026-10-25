package com.github.wuzhudexiaopacai.oj.judge.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wuzhudexiaopacai.oj.api.dto.ContestRecordDTO;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.judge.entity.ContestRecord;
import com.github.wuzhudexiaopacai.oj.judge.mapper.ContestRecordMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

/**
 * 比赛事件消费者：比赛结束时按赛制排序回写 rank_snapshot
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(consumerGroup = "ai-oj-contest-event-consumer",
        topic = MqConstants.CONTEST_EVENT_TOPIC,
        selectorExpression = "end")
public class ContestEventConsumer implements RocketMQListener<ContestEventConsumer.ContestEventMessage> {

    private final ContestRecordMapper contestRecordMapper;

    /**
     * 封榜快照写入 Redis（contest 服务读取 List&lt;ContestRecordDTO&gt;，api 共享类跨服务反序列化无类型风险）
     */
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(ContestEventMessage message) {
        if (!"end".equals(message.getEventType()) || message.getContestId() == null) {
            return;
        }
        Long contestId = message.getContestId();
        // contestType：0 ACM 1 OI（judge 服务无法查 contest 库，由比赛服务发消息时携带）
        boolean isOi = message.getContestType() != null && message.getContestType() == 1;
        log.info("比赛 {} 结束，开始结算 rank_snapshot，赛制={}", contestId, isOi ? "OI" : "ACM");

        List<ContestRecord> records = contestRecordMapper.selectList(
                new LambdaQueryWrapper<ContestRecord>().eq(ContestRecord::getContestId, contestId));
        if (records.isEmpty()) {
            return;
        }

        // ACM：acceptCount 降序 → penaltySeconds 升序；OI：totalScore 降序
        Comparator<ContestRecord> comparator = isOi
                ? Comparator.comparingInt(ContestRecord::getTotalScore).reversed()
                : Comparator.comparingInt(ContestRecord::getAcceptCount).reversed()
                .thenComparingLong(ContestRecord::getPenaltySeconds);
        List<ContestRecord> sorted = records.stream().sorted(comparator).toList();

        int rank = 1;
        for (ContestRecord record : sorted) {
            ContestRecord update = new ContestRecord();
            update.setId(record.getId());
            update.setRankSnapshot(rank++);
            contestRecordMapper.updateById(update);
        }
        log.info("比赛 {} rank_snapshot 结算完成，共 {} 人", contestId, sorted.size());

        // 写入封榜快照（contest 服务 CONTEST_RANK key 的读取契约：List<ContestRecordDTO>，按名次升序，永久保留）
        List<ContestRecordDTO> snapshot = sorted.stream().map(record -> {
            ContestRecordDTO dto = new ContestRecordDTO();
            dto.setContestId(record.getContestId());
            dto.setUserId(record.getUserId());
            dto.setUsername(record.getUsername());
            dto.setTotalScore(record.getTotalScore());
            dto.setAcceptCount(record.getAcceptCount());
            dto.setPenaltySeconds(record.getPenaltySeconds());
            return dto;
        }).toList();
        redisTemplate.opsForValue().set(RedisKeys.CONTEST_RANK + contestId, snapshot);
        log.info("比赛 {} 封榜快照已写入 Redis，共 {} 人", contestId, snapshot.size());
    }

    /**
     * 比赛事件消息体（contest 服务发布）
     */
    @Data
    public static class ContestEventMessage implements Serializable {

        /**
         * 事件类型：start|end|seal
         */
        private String eventType;

        private Long contestId;

        /**
         * 赛制：0 ACM 1 OI
         */
        private Integer contestType;

        private Long timestamp;
    }
}
