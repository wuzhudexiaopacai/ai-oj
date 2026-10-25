package com.github.wuzhudexiaopacai.oj.judge.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wuzhudexiaopacai.oj.api.dto.ContestRuntimeDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.JudgeResultMessage;
import com.github.wuzhudexiaopacai.oj.api.feign.ContestFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
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

import java.time.ZoneId;

/**
 * 比赛成绩消费者：判题结果更新 contest_record（排行榜读取时按赛制实时排序）
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

    private final ContestFeignClient contestFeignClient;

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
            // 罚时 = 首次 AC 提交时间 - 比赛开始时间（sec）；报名时间与开赛时间无关，不能作为基准
            JudgeSubmission submission = judgeSubmissionMapper.selectOne(new LambdaQueryWrapper<JudgeSubmission>()
                    .eq(JudgeSubmission::getSubmitId, message.getSubmitId()));
            if (submission != null && submission.getCreateTime() != null) {
                long startMillis = resolveContestStartMillis(message.getContestId(), record);
                long submitMillis = submission.getCreateTime()
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                record.setPenaltySeconds(record.getPenaltySeconds()
                        + Math.max((submitMillis - startMillis) / 1000, 0));
            }
        }
        // OI 模式总分累加（ACM 模式下该值由排行榜策略忽略）
        record.setTotalScore(record.getTotalScore() + (message.getScore() == null ? 0 : message.getScore()));
        contestRecordMapper.updateById(record);
    }

    /**
     * 获取比赛开始时间（epoch 毫秒）；比赛服务不可用时回退为报名时间并告警
     */
    private long resolveContestStartMillis(Long contestId, ContestRecord record) {
        try {
            Result<ContestRuntimeDTO> result = contestFeignClient.runtime(contestId, null);
            if (result != null && result.isSuccess() && result.getData() != null
                    && result.getData().getStartTimeMillis() != null) {
                return result.getData().getStartTimeMillis();
            }
        } catch (Exception e) {
            log.warn("查询比赛开始时间失败，罚时基准回退为报名时间 contestId={}", contestId, e);
        }
        return record.getCreateTime() == null
                ? System.currentTimeMillis()
                : record.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
