package com.github.wuzhudexiaopacai.oj.contest.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.enums.ContestStatusEnum;
import com.github.wuzhudexiaopacai.oj.contest.dto.ContestEventMessage;
import com.github.wuzhudexiaopacai.oj.contest.entity.Contest;
import com.github.wuzhudexiaopacai.oj.contest.mapper.ContestMapper;
import com.github.wuzhudexiaopacai.oj.contest.service.ContestRankService;
import com.github.wuzhudexiaopacai.oj.contest.service.ContestService;
import com.github.wuzhudexiaopacai.oj.contest.service.impl.ContestServiceImpl;
import com.github.wuzhudexiaopacai.oj.contest.vo.ContestTimeVO;
import com.github.wuzhudexiaopacai.oj.contest.websocket.ContestWsPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
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
     * RocketMQ 固定延时级别（1~18 级）对应的秒数，用于结算宽限期换算
     */
    private static final int[] DELAY_LEVEL_SECONDS =
            {1, 5, 10, 30, 60, 120, 180, 240, 300, 360, 420, 480, 540, 600, 1200, 1800, 3600, 7200};

    /**
     * 事件类型：结束（仅结束事件需要广播，判题服务据此结算榜单；开赛只做状态迁移与 WebSocket 推送）
     */
    private static final String EVENT_END = "end";

    private final ContestMapper contestMapper;

    private final RedissonClient redissonClient;

    private final RocketMQTemplate rocketMQTemplate;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ContestService contestService;

    private final ContestRankService contestRankService;

    private final ContestWsPushService wsPushService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 结算宽限期（秒）：比赛结束后延迟投递 end 事件，让截止前已提交但仍在排队/判题的提交先落库，
     * 避免最终榜单漏掉这些成绩；0 表示不延迟
     */
    @Value("${ai-oj.contest.settle-grace-seconds:300}")
    private long settleGraceSeconds;

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
            // 开赛无下游消费者，只做状态迁移与 WebSocket 推送；结束需要广播给判题服务结算榜单
            migrate(now, ContestStatusEnum.SCHEDULED, ContestStatusEnum.RUNNING,
                    Contest::getStartTime, null);
            migrate(now, ContestStatusEnum.RUNNING, ContestStatusEnum.ENDED,
                    Contest::getEndTime, EVENT_END);
            // 到达封榜时间的进行中比赛冻结一次榜单：封榜快照缺失时排行榜会降级为实时榜，普通用户仍能看到最新结果
            snapshotSealedContests(now);
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
     * @param eventType  迁移成功后广播的事件类型；为 null 表示不广播（开赛只推送 WebSocket）
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
            // 开赛/结束状态迁移实时推送做题页，前端据此切换倒计时阶段与提交可用性
            ContestTimeVO snapshot = contestService.timeSnapshot(contest.getId());
            if (snapshot != null) {
                wsPushService.publish(snapshot);
            }
            if (eventType != null) {
                sendContestEvent(eventType, contest);
            }
        }
    }

    /**
     * 为已到封榜时间的进行中比赛写入冻结快照（已写入的跳过，快照永久保留）
     *
     * @param now 当前时间
     */
    private void snapshotSealedContests(LocalDateTime now) {
        List<Contest> sealed = contestMapper.selectList(new LambdaQueryWrapper<Contest>()
                .eq(Contest::getStatus, ContestStatusEnum.RUNNING.name())
                .isNotNull(Contest::getSealTime)
                .le(Contest::getSealTime, now)
                .orderByAsc(Contest::getId)
                .last("LIMIT " + SCAN_BATCH_LIMIT));
        for (Contest contest : sealed) {
            try {
                contestRankService.writeSealSnapshotIfAbsent(contest.getId());
            } catch (Exception e) {
                // 单个比赛结算失败不影响其他比赛与本轮状态扫描
                log.error("封榜快照写入失败 contestId={}", contest.getId(), e);
            }
        }
    }

    /**
     * 发送比赛事件消息（topic:tag，消息体 JSON：{"eventType":"end","contestId":n,"contestType":0|1,"timestamp":n}）
     */
    private void sendContestEvent(String eventType, Contest contest) {
        ContestEventMessage message = new ContestEventMessage();
        message.setEventType(eventType);
        message.setContestId(contest.getId());
        message.setContestType(contest.getType());
        message.setTimestamp(System.currentTimeMillis());
        try {
            String destination = MqConstants.CONTEST_EVENT_TOPIC + ":" + eventType;
            String payload = objectMapper.writeValueAsString(message);
            // 结束事件延迟投递，给在途判题留出落库时间
            int delayLevel = EVENT_END.equals(eventType) ? delayLevelOf(settleGraceSeconds) : 0;
            if (delayLevel > 0) {
                rocketMQTemplate.syncSend(destination, MessageBuilder.withPayload(payload).build(),
                        5000, delayLevel);
            } else {
                rocketMQTemplate.syncSend(destination, payload);
            }
        } catch (Exception e) {
            // 消息发送失败不阻断状态迁移（状态已落库，事件丢失由 judge 侧兜底/人工补偿）
            log.error("比赛事件消息发送失败 contestId={} eventType={}", contest.getId(), eventType, e);
        }
    }

    /**
     * 秒 → RocketMQ 延时级别（向上取整）；0 表示不延迟，超过最大级别按最大级别
     *
     * @param delaySeconds 延迟秒数
     * @return 延时级别（0~18）
     */
    static int delayLevelOf(long delaySeconds) {
        if (delaySeconds <= 0) {
            return 0;
        }
        for (int i = 0; i < DELAY_LEVEL_SECONDS.length; i++) {
            if (delaySeconds <= DELAY_LEVEL_SECONDS[i]) {
                return i + 1;
            }
        }
        return DELAY_LEVEL_SECONDS.length;
    }
}
