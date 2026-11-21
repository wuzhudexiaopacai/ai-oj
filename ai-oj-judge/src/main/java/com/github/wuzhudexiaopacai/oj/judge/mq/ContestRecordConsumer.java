package com.github.wuzhudexiaopacai.oj.judge.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wuzhudexiaopacai.oj.api.dto.ContestRuntimeDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.JudgeResultMessage;
import com.github.wuzhudexiaopacai.oj.api.feign.ContestFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.judge.entity.ContestRecord;
import com.github.wuzhudexiaopacai.oj.judge.mapper.ContestRecordMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * 比赛成绩消费者：判题结果更新 contest_record（排行榜读取时按赛制实时排序）
 * <p>
 * 成绩一律从 judge_submission 重算，而不是对消息做增量累加：
 * <ul>
 *     <li>OI 每题取最高分，重复提交不会把分数越加越高</li>
 *     <li>消息重复/乱序不会重复计分，天然幂等，不再依赖 Redis 去重集合</li>
 *     <li>写库使用 GREATEST 单调合并，并发消费也不会丢更新</li>
 * </ul>
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(consumerGroup = MqConstants.GROUP_CONTEST_RECORD, topic = MqConstants.JUDGE_RESULT_TOPIC)
public class ContestRecordConsumer implements RocketMQListener<JudgeResultMessage> {

    private static final long PENALTY_PER_WRONG_SUBMISSION_SECONDS = 20 * 60L;

    private final ContestRecordMapper contestRecordMapper;

    private final JudgeSubmissionMapper judgeSubmissionMapper;

    private final ContestFeignClient contestFeignClient;

    @Override
    public void onMessage(JudgeResultMessage message) {
        // 非比赛提交直接 ack
        if (message.getContestId() == null || message.getContestId() == 0L) {
            return;
        }
        ContestRecord record = getOrInitRecord(message);
        if (record == null) {
            return;
        }

        int totalScore = nvl(judgeSubmissionMapper.selectBestScoreSumByContest(
                message.getContestId(), message.getUserId()));
        int acceptCount = nvl(judgeSubmissionMapper.selectSolvedCountByContest(
                message.getContestId(), message.getUserId()));

        // 仅有 AC 题目才产生罚时；AC 或计罚的错误结果都可能改变罚时（较早提交可能晚于 AC 返回）
        long penaltySeconds = record.getPenaltySeconds() == null ? 0L : record.getPenaltySeconds();
        if (acceptCount > 0 && shouldRecalculatePenalty(message.getStatusCode())) {
            Long startMillis = queryContestStartMillis(message.getContestId());
            if (startMillis == null) {
                log.warn("比赛开始时间不可用，本次仅更新得分与通过数 contestId={} userId={}",
                        message.getContestId(), message.getUserId());
            } else {
                penaltySeconds = calcPenaltySeconds(message.getContestId(), message.getUserId(), startMillis);
            }
        }

        // 重算值正常只会增大，GREATEST 防止并发/乱序消息把成绩改小
        contestRecordMapper.mergeScore(record.getId(), totalScore, acceptCount, penaltySeconds);
    }

    /**
     * 查询成绩记录；报名初始化异常导致记录缺失时兜底补建，避免成绩消息被静默丢弃
     */
    private ContestRecord getOrInitRecord(JudgeResultMessage message) {
        ContestRecord record = selectRecord(message.getContestId(), message.getUserId());
        if (record != null) {
            return record;
        }
        log.warn("比赛成绩记录缺失，按兜底逻辑补建 contestId={} userId={}",
                message.getContestId(), message.getUserId());
        ContestRecord created = new ContestRecord();
        created.setContestId(message.getContestId());
        created.setUserId(message.getUserId());
        created.setUsername("");
        created.setTotalScore(0);
        created.setAcceptCount(0);
        created.setPenaltySeconds(0L);
        try {
            contestRecordMapper.insert(created);
            return created;
        } catch (DuplicateKeyException e) {
            // 并发补建：另一条消息已先插入，回查即可
            return selectRecord(message.getContestId(), message.getUserId());
        }
    }

    private ContestRecord selectRecord(Long contestId, Long userId) {
        return contestRecordMapper.selectOne(new LambdaQueryWrapper<ContestRecord>()
                .eq(ContestRecord::getContestId, contestId)
                .eq(ContestRecord::getUserId, userId));
    }

    /**
     * 计算 ACM 罚时：每题首次通过时间 - 比赛开始时间，逐题累加（秒），
     * 并叠加各题首次通过前每次有效错误提交的 20 分钟罚时；编译错误与系统错误不计入。
     */
    private long calcPenaltySeconds(Long contestId, Long userId, long startMillis) {
        List<LocalDateTime> firstAcceptedTimes = judgeSubmissionMapper
                .selectFirstAcceptedTimesByContest(contestId, userId);
        if (firstAcceptedTimes == null || firstAcceptedTimes.isEmpty()) {
            return 0L;
        }
        long timePenalty = firstAcceptedTimes.stream()
                .filter(Objects::nonNull)
                .mapToLong(time -> Math.max(
                        (time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - startMillis) / 1000, 0))
                .sum();
        int wrongAttempts = nvl(judgeSubmissionMapper
                .selectWrongAttemptsBeforeFirstAccepted(contestId, userId));
        return timePenalty + wrongAttempts * PENALTY_PER_WRONG_SUBMISSION_SECONDS;
    }

    /**
     * 获取比赛开始时间（epoch 毫秒）；不可用时返回 null，本次不更新罚时，避免用报名时间算出错误罚时
     */
    private Long queryContestStartMillis(Long contestId) {
        try {
            Result<ContestRuntimeDTO> result = contestFeignClient.runtime(contestId, null);
            if (result != null && result.isSuccess() && result.getData() != null
                    && result.getData().getStartTimeMillis() != null) {
                return result.getData().getStartTimeMillis();
            }
        } catch (Exception e) {
            log.error("查询比赛开始时间失败 contestId={}", contestId, e);
        }
        return null;
    }

    private boolean shouldRecalculatePenalty(Integer statusCode) {
        if (statusCode == null) {
            return false;
        }
        return switch (JudgeStatusEnum.fromCode(statusCode)) {
            case ACCEPTED, WRONG_ANSWER, TIME_LIMIT_EXCEEDED, MEMORY_LIMIT_EXCEEDED,
                    RUNTIME_ERROR, PRESENTATION_ERROR, OUTPUT_LIMIT_EXCEEDED -> true;
            default -> false;
        };
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
