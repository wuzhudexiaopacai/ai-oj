package com.github.wuzhudexiaopacai.oj.judge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.wuzhudexiaopacai.oj.api.dto.ContestRuntimeDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.ProblemBriefDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.ContestFeignClient;
import com.github.wuzhudexiaopacai.oj.api.feign.ProblemFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.core.result.ResultCode;
import com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils;
import com.github.wuzhudexiaopacai.oj.judge.dto.CaseValidateRequest;
import com.github.wuzhudexiaopacai.oj.judge.dto.JudgeRunRequest;
import com.github.wuzhudexiaopacai.oj.judge.dto.JudgeSubmitRequest;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeCaseResult;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeOutbox;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSandbox;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeCaseResultMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeOutboxMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import com.github.wuzhudexiaopacai.oj.judge.mq.JudgeSubmitMessage;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxClient;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxLoadStrategy;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxRegistry;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.dto.SandboxRunRequest;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.dto.SandboxRunResponse;
import com.github.wuzhudexiaopacai.oj.judge.vo.CaseValidateVO;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    /**
     * 用例校验单组运行时间上限（毫秒）：用户可自定义，但必须封顶避免长期占用沙箱槽位
     */
    private static final int MAX_VALIDATE_TIME_LIMIT_MS = 30000;

    private final JudgeSubmissionMapper judgeSubmissionMapper;

    private final JudgeCaseResultMapper judgeCaseResultMapper;

    private final JudgeOutboxMapper judgeOutboxMapper;

    private final SandboxRegistry sandboxRegistry;

    private final RocketMQTemplate rocketMQTemplate;

    private final RedissonClient redissonClient;

    private final SandboxLoadStrategy sandboxLoadStrategy;

    private final SandboxClient sandboxClient;

    private final ContestFeignClient contestFeignClient;

    private final ProblemFeignClient problemFeignClient;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 允许提交的编程语言白名单，需与沙箱 languages.toml 的 name 保持一致
     */
    @Value("${ai-oj.judge.supported-languages:c,cpp,java,python3,go,javascript,rust}")
    private List<String> supportedLanguages;

    @Value("${ai-oj.judge.submit-interval-seconds:5}")
    private long submitIntervalSeconds;

    /**
     * 提交判题（用户级幂等 + 用户频率锁）
     * <p>
     * 幂等键为 (userId, submitId)：先按该组合查已有记录直接返回，插入时再以 uk_user_submit 兜底，
     * 网络重试/客户端重复提交只会产生一条提交记录，且不同用户的 submitId 互不影响。
     *
     * @param req 提交请求
     * @return 提交结果
     */
    public SubmitResultVO submit(JudgeSubmitRequest req) {
        Long userId = SecurityUtils.requireLogin().getId();
        // 幂等：同一用户重复提交同一 submitId，直接返回已有记录（不再重复校验比赛/重复投递消息）
        JudgeSubmission existing = findOwnSubmission(userId, req.getSubmitId());
        if (existing != null) {
            return new SubmitResultVO(existing.getId());
        }

        // 前置校验：语言白名单 + 题目存在且未下架，避免进入异步判题后才失败
        validateSubmitRequest(req);

        Long contestId = req.getContestId() == null ? 0L : req.getContestId();
        // 比赛提交前置校验：已报名 + 比赛进行中 + 题目属于该比赛，杜绝赛前/赛后刷分
        validateContestSubmission(contestId, req.getProblemId(), userId);

        // 防重：同用户对同题提交频率锁
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
            // 并发重试：首次请求可能刚落库，先按幂等键回查，避免把重试误判为「提交过于频繁」
            JudgeSubmission duplicated = findOwnSubmission(userId, req.getSubmitId());
            if (duplicated != null) {
                return new SubmitResultVO(duplicated.getId());
            }
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
            // 唯一键 uk_user_submit 兜底：并发重复提交时返回已落库的记录
            JudgeSubmission duplicated = findOwnSubmission(userId, req.getSubmitId());
            Asserts.notNull(duplicated, "提交记录不存在");
            return new SubmitResultVO(duplicated.getId());
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
     * 重判提交（管理端）：重置为排队中并清空上一轮结果，再重新投递判题消息
     * <p>
     * 适用于题目用例/SPJ/限制变更后需要重新评测历史提交；
     * 旧用例明细与结果 Outbox 一并清理，避免复判期间展示上一轮数据或结果消息被判定为已投递。
     * <p>
     * 仅允许重判终态提交；统计口径（用户/题目通过数）以下游按 submitId 幂等去重为准，
     * 重判结果不会重复累计提交数，也不会修正首次判题已计入的通过数。
     *
     * @param submissionId 提交记录 ID
     */
    public void rejudge(Long submissionId) {
        JudgeSubmission submission = judgeSubmissionMapper.selectById(submissionId);
        Asserts.notNull(submission, "提交记录不存在");
        // 仅允许重判已结束的提交：判题中的提交仍在占用沙箱，此时重置状态会导致重复判题与结果互相覆盖
        JudgeStatusEnum current = JudgeStatusEnum.fromCode(submission.getStatus());
        Asserts.notNull(current, "非法判题状态");
        Asserts.isTrue(current.isTerminal(), "提交仍在判题中，请等待结束后再重判");

        // updateById 会忽略 null 字段，这里必须用 UpdateWrapper 显式清空错误信息与判题时间
        judgeSubmissionMapper.update(null, new LambdaUpdateWrapper<JudgeSubmission>()
                .eq(JudgeSubmission::getId, submissionId)
                .set(JudgeSubmission::getStatus, JudgeStatusEnum.PENDING.getCode())
                .set(JudgeSubmission::getScore, 0)
                .set(JudgeSubmission::getTimeUsed, 0)
                .set(JudgeSubmission::getMemoryUsed, 0)
                .set(JudgeSubmission::getRetryCount, 0)
                .set(JudgeSubmission::getErrorMessage, null)
                .set(JudgeSubmission::getJudgeTime, null));

        judgeCaseResultMapper.delete(new LambdaQueryWrapper<JudgeCaseResult>()
                .eq(JudgeCaseResult::getSubmissionId, submissionId));
        judgeOutboxMapper.delete(new LambdaQueryWrapper<JudgeOutbox>()
                .eq(JudgeOutbox::getSubmitId, submission.getSubmitId()));
        // 清掉上一轮进度缓存：否则 WebSocket 断线的用户轮询会一直拿到旧终态，误以为重判已完成
        stringRedisTemplate.delete(
                RedisKeys.JUDGE_PROGRESS + submission.getUserId() + ":" + submission.getSubmitId());

        JudgeSubmitMessage message = JudgeSubmitMessage.builder()
                .submitId(submission.getSubmitId())
                .submissionId(submissionId)
                .build();
        rocketMQTemplate.syncSend(MqConstants.JUDGE_SUBMIT_TOPIC,
                MessageBuilder.withPayload(message)
                        .setHeader(RocketMQHeaders.KEYS, submission.getSubmitId())
                        .build(),
                5000);
        log.info("提交 {} 已重新入队判题", submission.getSubmitId());
    }

    /**
     * 按用户 + 幂等提交标识查询已有提交
     *
     * @param userId   提交用户
     * @param submitId 幂等提交标识
     * @return 已有提交，不存在返回 null
     */
    private JudgeSubmission findOwnSubmission(Long userId, String submitId) {
        return judgeSubmissionMapper.selectOne(new LambdaQueryWrapper<JudgeSubmission>()
                .eq(JudgeSubmission::getUserId, userId)
                .eq(JudgeSubmission::getSubmitId, submitId));
    }

    /**
     * 提交前置校验：语言白名单 + 题目存在且未下架
     * <p>
     * 非法请求必须在同步阶段拒绝：题目不存在/已下架要等到异步判题阶段才会暴露，
     * 期间会消耗业务重试次数并最终落入死信，用户体验与系统负载都很差。
     * 语言统一转小写存储，与沙箱 languages.toml 的 name 对齐。
     *
     * @param req 提交请求
     */
    private void validateSubmitRequest(JudgeSubmitRequest req) {
        req.setLanguage(normalizeSupportedLanguage(req.getLanguage()));

        Result<ProblemBriefDTO> result;
        try {
            result = problemFeignClient.getBrief(req.getProblemId());
        } catch (Exception e) {
            log.error("提交前置校验题目状态失败 problemId={}", req.getProblemId(), e);
            throw new BizException("题目服务暂不可用，请稍后重试");
        }
        if (result == null || !result.isSuccess()) {
            throw new BizException("题目服务暂不可用，请稍后重试");
        }
        ProblemBriefDTO brief = result.getData();
        Asserts.notNull(brief, "题目不存在");
        Asserts.isTrue(brief.getStatus() == null || brief.getStatus() != 0, "题目已下架，暂不可提交");
    }

    /**
     * 语言白名单校验并归一化为小写（与沙箱 languages.toml 的 name 对齐）
     * <p>
     * 所有调用沙箱的入口都必须先经过这里：沙箱对未知语言直接返回 400，
     * 若不提前拦截，调用方会把正常实例误判为故障并拉黑。
     *
     * @param language 原始语言
     * @return 归一化后的语言
     */
    private String normalizeSupportedLanguage(String language) {
        String normalized = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
        boolean supported = supportedLanguages != null && supportedLanguages.stream()
                .anyMatch(item -> item.equalsIgnoreCase(normalized));
        Asserts.isTrue(supported, "不支持的编程语言：" + language);
        return normalized;
    }

    /**
     * 沙箱调用失败是否需要拉黑实例
     * <p>
     * 4xx（参数/语言/令牌错误）与 429/503（繁忙）说明实例健康，只是本次请求不合法或排队失败，
     * 拉黑会让一次坏请求打掉正常实例；其余 5xx、网络超时、空响应才视为实例故障。
     *
     * @param e 沙箱调用异常
     * @return true 需要拉黑
     */
    private boolean shouldBlacklist(Exception e) {
        if (e instanceof WebClientResponseException responseException) {
            int status = responseException.getStatusCode().value();
            return !(status == 429 || status == 503 || (status >= 400 && status < 500));
        }
        return true;
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

        // 先校验语言再占用沙箱：未知语言会让沙箱返回 400，不能把健康实例误判为故障
        String language = normalizeSupportedLanguage(req.getLanguage());

        JudgeSandbox sandbox = selectAvailableSandbox();
        Asserts.notNull(sandbox, ResultCode.SANDBOX_UNAVAILABLE.getMessage());

        sandboxLoadStrategy.occupy(sandbox.getSandboxKey());
        try {
            SandboxRunResponse response = sandboxClient.run(sandbox.getBaseUrl(),
                    new SandboxRunRequest(language, req.getCode(), req.getInput(), 10000));
            JudgeStatusEnum status = mapStatus(response.status());
            return new JudgeRunVO(status.getCode(), status.getDesc(),
                    response.output(), response.errorMessage(), response.timeMs(), response.memoryKb());
        } catch (Exception e) {
            log.error("自测运行沙箱调用失败 sandbox={}", sandbox.getSandboxKey(), e);
            if (shouldBlacklist(e)) {
                sandboxLoadStrategy.blacklist(sandbox.getSandboxKey());
            }
            throw new BizException(ResultCode.SANDBOX_UNAVAILABLE);
        } finally {
            sandboxLoadStrategy.release(sandbox.getSandboxKey());
        }
    }

    /**
     * 选择一台在线沙箱（自测运行用，单次不重试）
     */
    private JudgeSandbox selectAvailableSandbox() {
        return sandboxLoadStrategy.select(sandboxRegistry.listAvailable());
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

    /**
     * 用例校验（AI 出题后用参考代码逐组实跑，以真实输出校正测试用例期望）
     * <p>
     * 不入库、不产生提交记录；选用一台在线沙箱，对每组用例调用 /run 跑参考代码得到真实输出，
     * 按“忽略行尾空白与末尾空行”规则与期望输出比对，回填每组真实输出供前端采用。
     * 编译失败时置 compileOk=false 并短路返回。
     *
     * @param req 校验请求
     * @return 逐组校验结果
     */
    public CaseValidateVO validateCases(CaseValidateRequest req) {
        Long userId = SecurityUtils.requireLogin().getId();

        String rateKey = "judge:validate:rate:" + userId;
        var rateLimiter = redissonClient.getRateLimiter(rateKey);
        // 校验为多组串行重操作，限流每用户每分钟 10 次
        rateLimiter.trySetRate(org.redisson.api.RateType.OVERALL, 10, 1, org.redisson.api.RateIntervalUnit.MINUTES);
        Asserts.isTrue(rateLimiter.tryAcquire(), "校验过于频繁，请稍后再试");

        // 同 run：未知语言会让沙箱返回 400，先拦截避免把健康实例拉黑
        String language = normalizeSupportedLanguage(req.getLanguage());

        JudgeSandbox sandbox = selectAvailableSandbox();
        Asserts.notNull(sandbox, ResultCode.SANDBOX_UNAVAILABLE.getMessage());

        // 限时由用户指定，必须设上限：超大值会长期占用沙箱槽位
        int timeLimit = req.getTimeLimitMs() == null || req.getTimeLimitMs() <= 0
                ? 10000 : Math.min(req.getTimeLimitMs(), MAX_VALIDATE_TIME_LIMIT_MS);

        CaseValidateVO vo = new CaseValidateVO();
        vo.setCompileOk(true);
        vo.setCases(new ArrayList<>());
        int passed = 0;

        sandboxLoadStrategy.occupy(sandbox.getSandboxKey());
        try {
            for (CaseValidateRequest.CaseItem item : req.getCases()) {
                CaseValidateVO.ItemResult r = new CaseValidateVO.ItemResult();
                r.setSeq(item.getSeq());
                r.setExpectedOutput(item.getExpectedOutput());
                try {
                    SandboxRunResponse resp = sandboxClient.run(sandbox.getBaseUrl(),
                            new SandboxRunRequest(language, req.getCode(), item.getInput(), timeLimit));
                    JudgeStatusEnum status = mapStatus(resp.status());
                    r.setStatus(status.name());
                    r.setActualOutput(resp.output());
                    r.setErrorMessage(resp.errorMessage());
                    r.setTimeMs(resp.timeMs());
                    r.setMemoryKb(resp.memoryKb());
                    if (status == JudgeStatusEnum.COMPILE_ERROR) {
                        vo.setCompileOk(false);
                        vo.setCompileLog(resp.errorMessage());
                        r.setPassed(false);
                        vo.getCases().add(r);
                        break;
                    }
                    // 仅在参考程序正常跑完时比较输出：超时/运行错误的程序也可能先输出正确内容，
                    // 只看文本会把这样的用例误判为校验通过
                    boolean ok = status == JudgeStatusEnum.ACCEPTED
                            && normalizeOutput(resp.output())
                            .equals(normalizeOutput(item.getExpectedOutput()));
                    r.setPassed(ok);
                    if (ok) {
                        passed++;
                    }
                } catch (Exception e) {
                    log.error("用例校验沙箱调用失败 seq={} sandbox={}", item.getSeq(), sandbox.getSandboxKey(), e);
                    if (shouldBlacklist(e)) {
                        sandboxLoadStrategy.blacklist(sandbox.getSandboxKey());
                    }
                    throw new BizException(ResultCode.SANDBOX_UNAVAILABLE);
                }
                vo.getCases().add(r);
            }
        } finally {
            sandboxLoadStrategy.release(sandbox.getSandboxKey());
        }

        vo.setTotalCount(req.getCases().size());
        vo.setPassedCount(passed);
        vo.setOverallPassed(Boolean.TRUE.equals(vo.getCompileOk()) && passed == vo.getTotalCount());
        return vo;
    }

    /**
     * 输出归一化：统一换行符、去除每行行尾空白与整体末尾空行，与标准答案比对规则一致
     */
    private static String normalizeOutput(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = t.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines[i].stripTrailing());
        }
        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == '\n') {
            end--;
        }
        return sb.substring(0, end);
    }
}
