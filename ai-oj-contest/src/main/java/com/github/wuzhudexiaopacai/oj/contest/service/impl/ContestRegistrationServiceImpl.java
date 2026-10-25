package com.github.wuzhudexiaopacai.oj.contest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wuzhudexiaopacai.oj.api.feign.JudgeFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.enums.ContestStatusEnum;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils;
import com.github.wuzhudexiaopacai.oj.contest.entity.Contest;
import com.github.wuzhudexiaopacai.oj.contest.entity.ContestRegistration;
import com.github.wuzhudexiaopacai.oj.contest.mapper.ContestMapper;
import com.github.wuzhudexiaopacai.oj.contest.mapper.ContestRegistrationMapper;
import com.github.wuzhudexiaopacai.oj.contest.service.ContestRegistrationService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 比赛报名服务实现
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContestRegistrationServiceImpl implements ContestRegistrationService {

    /**
     * 报名状态：已报名
     */
    private static final int STATUS_REGISTERED = 1;

    private final ContestMapper contestMapper;

    private final ContestRegistrationMapper registrationMapper;

    private final JudgeFeignClient judgeFeignClient;

    @Override
    @GlobalTransactional(name = "contest-register-tx", rollbackFor = Exception.class)
    public void register(Long contestId) {
        LoginUser loginUser = SecurityUtils.requireLogin();
        Contest contest = contestMapper.selectById(contestId);
        Asserts.notNull(contest, "比赛不存在");
        String status = contest.getStatus();
        Asserts.isTrue(ContestStatusEnum.SCHEDULED.name().equals(status)
                        || ContestStatusEnum.RUNNING.name().equals(status),
                "比赛已结束或已取消，无法报名");
        // 本地插入报名记录：数据库 uk_contest_user 唯一约束兜底幂等
        ContestRegistration registration = new ContestRegistration();
        registration.setContestId(contestId);
        registration.setUserId(loginUser.getId());
        registration.setStatus(STATUS_REGISTERED);
        try {
            registrationMapper.insert(registration);
        } catch (DuplicateKeyException e) {
            throw new BizException("已报名");
        }
        // Seata 全局事务参与方：远程初始化判题库中的比赛成绩记录，与报名记录同生共死；
        // 远程失败抛 BizException 触发 Seata 全局回滚，本地报名记录一并回滚
        Result<Void> result = judgeFeignClient.initContestRecord(contestId, loginUser.getId(), loginUser.getUsername());
        if (result == null || !result.isSuccess()) {
            log.error("初始化比赛成绩记录失败 contestId={} userId={} result={}", contestId, loginUser.getId(), result);
            throw new BizException("初始化比赛成绩记录失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long contestId) {
        LoginUser loginUser = SecurityUtils.requireLogin();
        ContestRegistration exist = registrationMapper.selectOne(new LambdaQueryWrapper<ContestRegistration>()
                .eq(ContestRegistration::getContestId, contestId)
                .eq(ContestRegistration::getUserId, loginUser.getId()));
        Asserts.notNull(exist, "未报名该比赛");
        // 物理删除：uk_contest_user 唯一约束要求取消后可再次报名（逻辑删除会残留数据行阻塞重新报名）
        registrationMapper.physicalDelete(contestId, loginUser.getId());
        // 说明：判题库中的比赛成绩记录（contest_record）保留不删除，用于历史追溯与审计；
        // 再次报名时判题侧 uk_contest_user 幂等，直接复用/重建成绩记录
    }
}
