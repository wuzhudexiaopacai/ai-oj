package com.github.wuzhudexiaopacai.oj.judge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wuzhudexiaopacai.oj.api.dto.ContestRuntimeDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.ContestFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.annotation.Idempotent;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.core.result.ResultCode;
import com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils;
import com.github.wuzhudexiaopacai.oj.judge.dto.JudgeRunRequest;
import com.github.wuzhudexiaopacai.oj.judge.dto.JudgeSubmitRequest;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSandbox;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSandboxMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import com.github.wuzhudexiaopacai.oj.judge.mq.JudgeSubmitMessage;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxClient;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxLoadStrategy;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.dto.SandboxRunRequest;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.dto.SandboxRunResponse;
import com.github.wuzhudexiaopacai.oj.judge.vo.JudgeRunVO;
import com.github.wuzhudexiaopacai.oj.judge.vo.SubmitResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 判题提交/自测运行服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeService {

    private final JudgeSubmissionMapper judgeSubmissionMapper;

    private final JudgeSandboxMapper judgeSandboxMapper;

    private final RocketMQTemplate rocketMQTemplate;

    private final RedissonClient redissonClient;

    private final SandboxLoadStrategy sandboxLoadStrategy;

    private final SandboxClient sandboxClient;

    private final ContestFeignClient contestFeignClient;

    @Value("${ai-oj.judge.submit-interval-seconds:5}")
    private long submitIntervalSeconds;

    /**
     * 提交判题（三层幂等：注解幂等 + 用户频率锁 + 数据库唯一键）
     *
     * @param req 提交请求
     * @return 提交结果
     */
    @Idempotent(key = "#req.submitId", expire = 30)
    public SubmitResultVO submit(JudgeSubmitRequest req) {
        Long userId = SecurityUtils.requireLogin().getId();
        Long contestId = req.getContestId() == null ? 0L : req.getContestId();
        // 比赛提交前置校验：已报名 + 比赛进行中 + 题目属于该比赛，杜绝赛前/赛后刷分
        validateContestSubmission(contestId, req.getProblemId(), userId);

        // 第二层防重：同用户对同题提交频率锁
        String lockKey = RedisKeys.JUDGE_USER_LOCK + userId + ":" + req.getProblemId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked;
        try {
            locked = lock.tryLock(0, submitIntervalSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.FAILED);
        }
        if (!locked) {
            throw new BizException(ResultCode.CONFLICT.getCode(), "提交过于频繁");
        }

        JudgeSubmission submission = new JudgeSubmission();
        submission.setSubmitId(req.getSubmitId());
        submission.setUserId(userId);
        submission.setProblemId(req.getProblemId());
        submission.setContestId(contestId);
        submission.setLanguage(req.getLanguage());
        submission.setCode(req.getCode());
        submission.setCodeLength(req.getCode().length());
        submission.setStatus(JudgeStatusEnum.PENDING.getCode());
        submission.setScore(0);
        submission.setTimeUsed(0);
        submission.setMemoryUsed(0);
        submission.setRetryCount(0);

        try {
            judgeSubmissionMapper.insert(submission);
        } catch (DuplicateKeyException e) {
            // 第三层幂等：uk_submit_id 兜底，重复提交直接返回已有记录
            JudgeSubmission existing = judgeSubmissionMapper.selectOne(
                    new LambdaQueryWrapper<JudgeSubmission>().eq(JudgeSubmission::getSubmitId, req.getSubmitId()));
            Asserts.notNull(existing, "提交记录不存在");
            return new SubmitResultVO(existing.getId());
        }

        // 投递判题消息（keys=submitId 便于追踪）
        JudgeSubmitMessage message = JudgeSubmitMessage.builder()
                .submitId(req.getSubmitId())
                .submissionId(submission.getId())
                .build();
        rocketMQTemplate.syncSend(MqConstants.JUDGE_SUBMIT_TOPIC,
                MessageBuilder.withPayload(message)
                        .setHeader(RocketMQHeaders.KEYS, req.getSubmitId())
                        .build(),
                5000);

        return new SubmitResultVO(submission.getId());
    }

    /**
     * 校验比赛提交：已报名 + 比赛进行中 + 题目属于该比赛
     * <p>
     * 比赛成绩独立计算：只有比赛进行期间、带本次比赛 ID 的提交才会计入成绩，
     * 赛前做过的题不会自动继承 AC，需要在比赛内重新提交。
     *
     * @param contestId 比赛 ID（0 表示非比赛提交，直接放行）
     * @param problemId 题目 ID
     * @param userId    提交用户
     */
    private void validateContestSubmission(Long contestId, Long problemId, Long userId) {
        if (contestId == null || contestId == 0L) {
            return;
        }
        Result<ContestRuntimeDTO> result;
        try {
            result = contestFeignClient.runtime(contestId, userId);
        } catch (Exception e) {
            log.error("查询比赛运行时信息失败 contestId={}", contestId, e);
            throw new BizException("比赛服务暂不可用，请稍后重试");
        }
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BizException("比赛不存在");
        }
        ContestRuntimeDTO runtime = result.getData();
        Asserts.isTrue(!"CANCELED".equals(runtime.getStatus()), "比赛已取消");
        Asserts.isTrue(Boolean.TRUE.equals(runtime.getRegistered()), "未报名该比赛，无法提交");
        long now = System.currentTimeMillis();
        Asserts.isTrue(runtime.getStartTimeMillis() == null || now >= runtime.getStartTimeMillis(),
                "比赛尚未开始");
        Asserts.isTrue(runtime.getEndTimeMillis() == null || now < runtime.getEndTimeMillis(),
                "比赛已结束");
        Asserts.isTrue(runtime.getProblemIds() != null && runtime.getProblemIds().contains(problemId),
                "该题目不属于本场比赛");
    }

    /**
     * 自测运行（不入库，直接调沙箱 /run；Redisson 限流每用户每分钟 20 次）
     *
     * @param req 运行请求
     * @return 运行结果
     */
    public JudgeRunVO run(JudgeRunRequest req) {
        Long userId = SecurityUtils.requireLogin().getId();

        String rateKey = "judge:run:rate:" + userId;
        var rateLimiter = redissonClient.getRateLimiter(rateKey);
        // 每用户每分钟 20 次（限流器幂等初始化）
        rateLimiter.trySetRate(org.redisson.api.RateType.OVERALL, 20, 1, org.redisson.api.RateIntervalUnit.MINUTES);
        Asserts.isTrue(rateLimiter.tryAcquire(), "运行过于频繁，请稍后再试");

        JudgeSandbox sandbox = selectAvailableSandbox();
        Asserts.notNull(sandbox, ResultCode.SANDBOX_UNAVAILABLE.getMessage());

        sandboxLoadStrategy.occupy(sandbox.getSandboxKey());
        try {
            SandboxRunResponse response = sandboxClient.run(sandbox.getBaseUrl(),
                    new SandboxRunRequest(req.getLanguage(), req.getCode(), req.getInput(), 10000));
            JudgeStatusEnum status = mapStatus(response.status());
            return new JudgeRunVO(status.getCode(), status.getDesc(),
                    response.output(), response.errorMessage(), response.timeMs(), response.memoryKb());
        } catch (Exception e) {
            log.error("自测运行沙箱调用失败 sandbox={}", sandbox.getSandboxKey(), e);
            sandboxLoadStrategy.blacklist(sandbox.getSandboxKey());
            throw new BizException(ResultCode.SANDBOX_UNAVAILABLE);
        } finally {
            sandboxLoadStrategy.release(sandbox.getSandboxKey());
        }
    }

    /**
     * 选择一台在线沙箱（自测运行用，单次不重试）
     */
    private JudgeSandbox selectAvailableSandbox() {
        List<JudgeSandbox> sandboxes = judgeSandboxMapper.selectList(
                new LambdaQueryWrapper<JudgeSandbox>().eq(JudgeSandbox::getStatus, 1));
        return sandboxLoadStrategy.select(sandboxes);
    }

    /**
     * 沙箱状态字符串 → JudgeStatusEnum
     * 沙箱返回全名（ACCEPTED/WRONG_ANSWER/...），兼容旧短码（AC/WA/...）
     */
    public static JudgeStatusEnum mapStatus(String status) {
        if (status == null) {
            return JudgeStatusEnum.SYSTEM_ERROR;
        }
        return switch (status) {
            case "ACCEPTED", "AC" -> JudgeStatusEnum.ACCEPTED;
            case "WRONG_ANSWER", "WA" -> JudgeStatusEnum.WRONG_ANSWER;
            case "TIME_LIMIT_EXCEEDED", "TLE" -> JudgeStatusEnum.TIME_LIMIT_EXCEEDED;
            case "MEMORY_LIMIT_EXCEEDED", "MLE" -> JudgeStatusEnum.MEMORY_LIMIT_EXCEEDED;
            case "RUNTIME_ERROR", "RE" -> JudgeStatusEnum.RUNTIME_ERROR;
            case "COMPILE_ERROR", "CE" -> JudgeStatusEnum.COMPILE_ERROR;
            case "PRESENTATION_ERROR", "PE" -> JudgeStatusEnum.PRESENTATION_ERROR;
            case "OUTPUT_LIMIT_EXCEEDED", "OLE" -> JudgeStatusEnum.OUTPUT_LIMIT_EXCEEDED;
            default -> JudgeStatusEnum.SYSTEM_ERROR;
        };
    }
}
