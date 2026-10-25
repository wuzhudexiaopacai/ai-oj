package com.github.wuzhudexiaopacai.oj.judge.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wuzhudexiaopacai.oj.api.dto.JudgeResultMessage;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import com.github.wuzhudexiaopacai.oj.judge.entity.ContestRecord;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import com.github.wuzhudexiaopacai.oj.judge.mapper.ContestRecordMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 比赛成绩消费者：判题结果更新 contest_record + 实时榜 ZSet
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(consumerGroup = MqConstants.GROUP_CONTEST_RECORD, topic = MqConstants.JUDGE_RESULT_TOPIC)
public class ContestRecordConsumer implements RocketMQListener<JudgeResultMessage> {

    /**
     * 比赛已解题集合前缀：+ contestId:userId，member=problemId
     */
    private static final String CONTEST_SOLVED_KEY = "contest:solved:";

    private final ContestRecordMapper contestRecordMapper;

    private final JudgeSubmissionMapper judgeSubmissionMapper;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void onMessage(JudgeResultMessage message) {
        // 非比赛提交直接 ack
        if (message.getContestId() == null || message.getContestId() == 0L) {
            return;
        }

        ContestRecord record = contestRecordMapper.selectOne(new LambdaQueryWrapper<ContestRecord>()
                .eq(ContestRecord::getContestId, message.getContestId())
                .eq(ContestRecord::getUserId, message.getUserId()));
        if (record == null) {
            log.warn("比赛成绩记录不存在 contestId={} userId={}", message.getContestId(), message.getUserId());
            return;
        }

        boolean isAc = JudgeStatusEnum.ACCEPTED.getCode() == message.getStatusCode();
        // 同一用户同一题去重：已 AC 过的题不再重复加分/计罚时
        String solvedKey = CONTEST_SOLVED_KEY + message.getContestId() + ":" + message.getUserId();
        Boolean alreadySolved = stringRedisTemplate.opsForSet()
                .isMember(solvedKey, message.getProblemId().toString());
        if (Boolean.TRUE.equals(alreadySolved)) {
            log.info("用户 {} 在比赛 {} 中已 AC 题目 {}，跳过计分", message.getUserId(), message.getContestId(), message.getProblemId());
            return;
        }

        if (isAc) {
            stringRedisTemplate.opsForSet().add(solvedKey, message.getProblemId().toString());
            record.setAcceptCount(record.getAcceptCount() + 1);
            // 罚时取舍说明：消息体不含比赛开赛时间，回查 contest 服务成本高；
            // 用 judge_submission.create_time 与 contest_record.create_time 差值近似「距开赛秒数」，
            // 前提是 contest_record 在比赛开始时初始化（报名时 Seata 事务写入），存在一定误差。
            JudgeSubmission submission = judgeSubmissionMapper.selectOne(new LambdaQueryWrapper<JudgeSubmission>()
                    .eq(JudgeSubmission::getSubmitId, message.getSubmitId()));
            if (submission != null && record.getCreateTime() != null && submission.getCreateTime() != null) {
                long penalty = Duration.between(record.getCreateTime(), submission.getCreateTime()).getSeconds();
                record.setPenaltySeconds(record.getPenaltySeconds() + Math.max(penalty, 0));
            }
        }
        // OI 模式总分累加（ACM 模式下该值由排行榜策略忽略）
        record.setTotalScore(record.getTotalScore() + (message.getScore() == null ? 0 : message.getScore()));
        contestRecordMapper.updateById(record);

        // 刷新实时榜 ZSet：ACM = acceptCount * 1e9 - penaltySeconds（通过数优先，同数罚时少者优先）；
        // OI = totalScore。公式注释便于 contest 服务直接 ZREVRANGE。
        double rankScore = (double) record.getAcceptCount() * 1e9 - record.getPenaltySeconds();
        stringRedisTemplate.opsForZSet().add(RedisKeys.CONTEST_RANK_ZSET + message.getContestId(),
                message.getUserId().toString(), rankScore);
    }
}
