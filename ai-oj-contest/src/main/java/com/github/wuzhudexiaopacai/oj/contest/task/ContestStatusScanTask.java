package com.github.wuzhudexiaopacai.oj.contest.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.enums.ContestStatusEnum;
import com.github.wuzhudexiaopacai.oj.contest.dto.ContestEventMessage;
import com.github.wuzhudexiaopacai.oj.contest.entity.Contest;
import com.github.wuzhudexiaopacai.oj.contest.mapper.ContestMapper;
import com.github.wuzhudexiaopacai.oj.contest.service.impl.ContestServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 比赛状态机扫描任务：驱动 SCHEDULED→RUNNING→ENDED 迁移，
 * 迁移成功后发送 RocketMQ 比赛事件（CONTEST_EVENT_TOPIC，tag=start|end）
 * <p>
 * 多实例部署时通过 Redisson 分布式锁（看门狗自动续期）保证同一时刻仅一个实例执行扫描；
 * 状态更新使用 MyBatis-Plus 乐观锁，更新失败说明已被并发迁移/修改，直接跳过。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContestStatusScanTask {

    /**
     * 扫描分布式锁 key（看门狗续期，任务挂掉后自动释放）
     */
    private static final String SCAN_LOCK_KEY = "contest:lock:scan";

    /**
     * 单次扫描每类迁移的最大处理条数（防长事务/长任务）
     */
    private static final int SCAN_BATCH_LIMIT = 100;

    /**
     * 事件类型：开赛
     */
    private static final String EVENT_START = "start";

    /**
     * 事件类型：结束
     */
    private static final String EVENT_END = "end";

    private final ContestMapper contestMapper;

    private final RedissonClient redissonClient;

    private final RocketMQTemplate rocketMQTemplate;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 定时扫描（间隔取配置 ai-oj.contest.status-scan-interval-ms，默认 5s）
     */
    @Scheduled(fixedDelayString = "${ai-oj.contest.status-scan-interval-ms:5000}")
    public void scan() {
        RLock lock = redissonClient.getLock(SCAN_LOCK_KEY);
        // tryLock 不等待：未抢到锁说明其他实例正在执行，直接跳过本周期；看门狗保证持锁期间自动续期
        if (!lock.tryLock()) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            // 先迁移开赛（SCHEDULED→RUNNING），再迁移结束（RUNNING→ENDED），保证超短比赛两个事件按序发出
            migrate(now, ContestStatusEnum.SCHEDULED, ContestStatusEnum.RUNNING,
                    Contest::getStartTime, EVENT_START);
            migrate(now, ContestStatusEnum.RUNNING, ContestStatusEnum.ENDED,
                    Contest::getEndTime, EVENT_END);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 批量迁移状态
     *
     * @param now        当前时间
     * @param from       源状态
     * @param to         目标状态
     * @param timeGetter 触发时间字段（start_time / end_time）
     * @param eventType  迁移成功后广播的事件类型
     */
    private void migrate(LocalDateTime now, ContestStatusEnum from, ContestStatusEnum to,
                         SFunction<Contest, LocalDateTime> timeGetter, String eventType) {
        List<Contest> contests = contestMapper.selectList(new LambdaQueryWrapper<Contest>()
                .eq(Contest::getStatus, from.name())
                .le(timeGetter, now)
                .orderByAsc(Contest::getId)
                .last("LIMIT " + SCAN_BATCH_LIMIT));
        for (Contest contest : contests) {
            Contest update = new Contest();
            update.setId(contest.getId());
            update.setStatus(to.name());
            // 乐观锁：携带扫描时读到的版本号，更新失败说明已被并发迁移/修改，跳过等下周期
            update.setVersion(contest.getVersion());
            int rows = contestMapper.updateById(update);
            if (rows == 0) {
                log.info("比赛状态迁移冲突跳过 contestId={} {}->{}", contest.getId(), from, to);
                continue;
            }
            // 状态变更属于写操作，主动失效详情缓存
            redisTemplate.delete(ContestServiceImpl.CONTEST_INFO_CACHE + contest.getId());
            sendContestEvent(eventType, contest);
        }
    }

    /**
     * 发送比赛事件消息（topic:tag，消息体 JSON：{"eventType":"start|end","contestId":n,"contestType":0|1,"timestamp":n}）
     */
    private void sendContestEvent(String eventType, Contest contest) {
        ContestEventMessage message = new ContestEventMessage();
        message.setEventType(eventType);
        message.setContestId(contest.getId());
        message.setContestType(contest.getType());
        message.setTimestamp(System.currentTimeMillis());
        try {
            rocketMQTemplate.syncSend(MqConstants.CONTEST_EVENT_TOPIC + ":" + eventType,
                    objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            // 消息发送失败不阻断状态迁移（状态已落库，事件丢失由 judge 侧兜底/人工补偿）
            log.error("比赛事件消息发送失败 contestId={} eventType={}", contest.getId(), eventType, e);
        }
    }
}
