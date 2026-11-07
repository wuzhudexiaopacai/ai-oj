package com.github.wuzhudexiaopacai.oj.judge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wuzhudexiaopacai.oj.api.dto.ContestRecordDTO;
import com.github.wuzhudexiaopacai.oj.judge.entity.ContestRecord;
import com.github.wuzhudexiaopacai.oj.judge.mapper.ContestRecordMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 比赛成绩服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContestRecordService {

    private final ContestRecordMapper contestRecordMapper;

    private final JudgeSubmissionMapper judgeSubmissionMapper;

    /**
     * 初始化比赛成绩记录（Seata AT 参与方；uk_contest_user 幂等，重复直接成功）
     *
     * @param contestId 比赛 ID
     * @param userId    用户 ID
     * @param username  用户名快照
     */
    @Transactional(rollbackFor = Exception.class)
    public void initRecord(Long contestId, Long userId, String username) {
        ContestRecord record = new ContestRecord();
        record.setContestId(contestId);
        record.setUserId(userId);
        record.setUsername(username == null ? "" : username);
        record.setTotalScore(0);
        record.setAcceptCount(0);
        record.setPenaltySeconds(0L);
        try {
            contestRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // uk_contest_user 幂等：重复报名初始化直接成功
            log.info("比赛成绩记录已存在，幂等跳过 contestId={} userId={}", contestId, userId);
        }
    }

    /**
     * 查询比赛成绩列表（只返回比赛中有提交记录的用户，未参赛的报名者不进入排行榜）
     *
     * @param contestId 比赛 ID
     * @return 成绩列表
     */
    public List<ContestRecordDTO> listByContest(Long contestId) {
        Set<Long> participants = judgeSubmissionMapper.selectUserIdsByContest(contestId).stream()
                .collect(Collectors.toSet());
        if (participants.isEmpty()) {
            return List.of();
        }
        List<ContestRecord> records = contestRecordMapper.selectList(
                new LambdaQueryWrapper<ContestRecord>()
                        .eq(ContestRecord::getContestId, contestId)
                        .in(ContestRecord::getUserId, participants));
        return records.stream().map(r -> {
            ContestRecordDTO dto = new ContestRecordDTO();
            dto.setContestId(r.getContestId());
            dto.setUserId(r.getUserId());
            dto.setUsername(r.getUsername());
            dto.setTotalScore(r.getTotalScore());
            dto.setAcceptCount(r.getAcceptCount());
            dto.setPenaltySeconds(r.getPenaltySeconds());
            dto.setRankSnapshot(r.getRankSnapshot());
            return dto;
        }).toList();
    }
}
