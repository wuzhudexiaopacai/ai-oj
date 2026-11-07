package com.github.wuzhudexiaopacai.oj.judge.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.api.dto.JudgeCaseDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.JudgeResultMessage;
import com.github.wuzhudexiaopacai.oj.api.dto.ProblemJudgeDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.FileFeignClient;
import com.github.wuzhudexiaopacai.oj.api.feign.ProblemFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeCaseResult;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSandbox;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeCaseResultMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import com.github.wuzhudexiaopacai.oj.judge.mq.JudgeSubmitMessage;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxClient;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxLoadStrategy;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxRegistry;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.dto.SandboxJudgeRequest;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.dto.SandboxJudgeResponse;
import com.github.wuzhudexiaopacai.oj.judge.statemachine.JudgeEvent;
import com.github.wuzhudexiaopacai.oj.judge.statemachine.JudgeStateMachineService;
import com.github.wuzhudexiaopacai.oj.judge.vo.JudgeProgressVO;
import com.github.wuzhudexiaopacai.oj.judge.websocket.JudgeWsPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 判题执行核心服务（供消费者调用）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeExecuteService {

    /**
     * 题目判题信息缓存前缀
     */
    private static final String PROBLEM_INFO_CACHE = "judge:problem:info:";

    /**
     * 题目判题信息缓存 TTL（秒），降 Feign 压力
     */
    private static final long PROBLEM_INFO_TTL = 60L;

    /**
     * 重投递延时级别（RocketMQ 延时级别 3 = 10s）
     */
    private static final int RETRY_DELAY_LEVEL = 3;

    /**
     * 题目未配置 stack_limit 时的默认栈空间（MB）
     */
    private static final int DEFAULT_STACK_LIMIT_MB = 256;

    private final JudgeSubmissionMapper judgeSubmissionMapper;

    private final JudgeCaseResultMapper judgeCaseResultMapper;

    private final SandboxRegistry sandboxRegistry;

    private final JudgeStateMachineService stateMachineService;

    private final ProblemFeignClient problemFeignClient;

    private final FileFeignClient fileFeignClient;

    private final SandboxLoadStrategy sandboxLoadStrategy;

    private final SandboxClient sandboxClient;

    private final RocketMQTemplate rocketMQTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    private final JudgeWsPushService judgeWsPushService;

    @Value("${ai-oj.judge.progress-ttl-seconds:3600}")
    private long progressTtlSeconds;

    /**
     * 执行判题主流程
     *
     * @param submissionId 提交记录 ID
     */
    public void execute(Long submissionId) {
        JudgeSubmission submission = judgeSubmissionMapper.selectById(submissionId);
        // 幂等：不存在/已终态直接跳过
        if (submission == null) {
            log.warn("提交记录不存在 submissionId={}", submissionId);
            return;
        }
        JudgeStatusEnum current = JudgeStatusEnum.fromCode(submission.getStatus());
        if (current.isTerminal()) {
            log.info("提交 {} 已终态 {}，跳过重复消费", submission.getSubmitId(), current);
            return;
        }

        try {
            // 状态机：PENDING → COMPILING
            // MQ 重投时 submission 可能已是 COMPILING/JUDGING，按当前状态跳过已完成迁移，保证重入安全
            if (current == JudgeStatusEnum.PENDING) {
                stateMachineService.sendEvent(submission, JudgeEvent.COMPILE_START, null);
                current = JudgeStatusEnum.COMPILING;
            }

            // 拉取题目判题信息（Redis 缓存 60s）
            ProblemJudgeDTO problem = loadProblemInfo(submission.getProblemId());

            // 状态机：COMPILING → JUDGING
            if (current == JudgeStatusEnum.COMPILING) {
                submission = judgeSubmissionMapper.selectById(submissionId);
                stateMachineService.sendEvent(submission, JudgeEvent.JUDGE_START, null);
            }

            // 选择沙箱并调用（失败拉黑换机重试，最多 JUDGE_MAX_RETRY 次）
            SandboxJudgeResponse response = invokeSandboxWithRetry(submission, problem);

            // 落库终态 + 用例明细
            submission = judgeSubmissionMapper.selectById(submissionId);
            persistResult(submission, response);

            // 广播结果
            broadcast(submission);
        } catch (Exception e) {
            log.error("判题执行异常 submitId={}", submission.getSubmitId(), e);
            handleExecuteFailure(submission, e);
        }
    }

    /**
     * 拉取题目判题信息（Redis 缓存 60s 降 Feign 压力）
     */
    private ProblemJudgeDTO loadProblemInfo(Long problemId) {
        String cacheKey = PROBLEM_INFO_CACHE + problemId;
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, ProblemJudgeDTO.class);
            }
        } catch (Exception e) {
            log.warn("题目信息缓存读取失败 problemId={}", problemId, e);
        }

        Result<ProblemJudgeDTO> result = problemFeignClient.getJudgeInfo(problemId);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BizException("拉取题目判题信息失败 problemId=" + problemId);
        }
        ProblemJudgeDTO problem = result.getData();

        // 用例内容补全：input 为空且 inputFile 非空 → 从 MinIO 拉取（output 同理）
        if (problem.getCases() != null) {
            for (JudgeCaseDTO judgeCase : problem.getCases()) {
                if (StrUtil.isBlank(judgeCase.getInput()) && StrUtil.isNotBlank(judgeCase.getInputFile())) {
                    judgeCase.setInput(loadFileContent(judgeCase.getInputFile()));
                }
                if (StrUtil.isBlank(judgeCase.getOutput()) && StrUtil.isNotBlank(judgeCase.getOutputFile())) {
                    judgeCase.setOutput(loadFileContent(judgeCase.getOutputFile()));
                }
            }
        }

        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(problem),
                    Duration.ofSeconds(PROBLEM_INFO_TTL));
        } catch (Exception e) {
            log.warn("题目信息缓存写入失败 problemId={}", problemId, e);
        }
        return problem;
    }

    /**
     * 从文件服务拉取用例内容
     */
    private String loadFileContent(String objectKey) {
        Result<String> result = fileFeignClient.getContent(objectKey);
        if (result == null || !result.isSuccess()) {
            throw new BizException("拉取用例文件失败 objectKey=" + objectKey);
        }
        return result.getData();
    }

    /**
     * 选择沙箱并调用，失败拉黑换机重试，最多 JUDGE_MAX_RETRY 次
     */
    private SandboxJudgeResponse invokeSandboxWithRetry(JudgeSubmission submission, ProblemJudgeDTO problem) {
        List<JudgeSandbox> sandboxes = sandboxRegistry.listAvailable();
        if (sandboxes == null || sandboxes.isEmpty()) {
            throw new BizException("无可用评测沙箱");
        }

        SandboxJudgeRequest request = buildSandboxRequest(submission, problem);
        List<String> triedKeys = new ArrayList<>();
        Exception lastException = null;

        for (int i = 0; i < MqConstants.JUDGE_MAX_RETRY; i++) {
            List<JudgeSandbox> candidates = sandboxes.stream()
                    .filter(s -> !triedKeys.contains(s.getSandboxKey()))
                    .toList();
            JudgeSandbox sandbox = sandboxLoadStrategy.select(candidates);
            if (sandbox == null) {
                break;
            }
            triedKeys.add(sandbox.getSandboxKey());
            sandboxLoadStrategy.occupy(sandbox.getSandboxKey());
            try {
                return sandboxClient.judge(sandbox.getBaseUrl(), request);
            } catch (Exception e) {
                lastException = e;
                log.error("沙箱调用失败，拉黑并换机重试 sandbox={} attempt={}", sandbox.getSandboxKey(), i + 1, e);
                sandboxLoadStrategy.blacklist(sandbox.getSandboxKey());
            } finally {
                sandboxLoadStrategy.release(sandbox.getSandboxKey());
            }
        }
        throw new BizException("全部沙箱调用失败：" + (lastException == null ? "无可用沙箱" : lastException.getMessage()));
    }

    /**
     * 组装沙箱请求
     */
    private SandboxJudgeRequest buildSandboxRequest(JudgeSubmission submission, ProblemJudgeDTO problem) {
        List<SandboxJudgeRequest.CaseItem> cases = problem.getCases() == null ? List.of()
                : problem.getCases().stream()
                .map(c -> new SandboxJudgeRequest.CaseItem(c.getSeq(), c.getInput(), c.getOutput(), c.getScore()))
                .toList();
        SandboxJudgeRequest.SpjItem spj = Boolean.TRUE.equals(problem.getIsSpj())
                ? new SandboxJudgeRequest.SpjItem(problem.getSpjLanguage(), problem.getSpjCode())
                : null;
        return new SandboxJudgeRequest(submission.getId(), submission.getLanguage(), submission.getCode(),
                problem.getTimeLimit(), problem.getMemoryLimit(),
                resolveStackLimit(problem), Boolean.TRUE.equals(problem.getIoScore()), cases, spj);
    }

    /**
     * 题目未配置栈限制时回落 256MB
     *
     * @param problem 题目判题信息
     * @return 栈空间限制（MB）
     */
    private Integer resolveStackLimit(ProblemJudgeDTO problem) {
        return problem.getStackLimit() == null || problem.getStackLimit() <= 0
                ? DEFAULT_STACK_LIMIT_MB
                : problem.getStackLimit();
    }

    /**
     * 落库终态 + 用例明细 + 状态机迁移
     */
    private void persistResult(JudgeSubmission submission, SandboxJudgeResponse response) {
        JudgeStatusEnum finalStatus = JudgeService.mapStatus(response.overallStatus());
        int maxTime = 0;
        int maxMemory = 0;
        List<JudgeCaseResult> caseResults = new ArrayList<>();
        if (response.cases() != null) {
            for (SandboxJudgeResponse.CaseResult c : response.cases()) {
                JudgeCaseResult caseResult = new JudgeCaseResult();
                caseResult.setSubmissionId(submission.getId());
                caseResult.setSeq(c.seq());
                caseResult.setStatus(JudgeService.mapStatus(c.status()).getCode());
                caseResult.setTimeUsed(c.timeMs() == null ? 0 : c.timeMs());
                caseResult.setMemoryUsed(c.memoryKb() == null ? 0 : c.memoryKb());
                caseResult.setScore(c.score() == null ? 0 : c.score());
                caseResult.setOutputPreview(StrUtil.subPre(c.outputPreview(), 1024));
                caseResults.add(caseResult);
                maxTime = Math.max(maxTime, caseResult.getTimeUsed());
                maxMemory = Math.max(maxMemory, caseResult.getMemoryUsed());
            }
        }

        // 终态字段先组装到实体上，由状态机 COMPLETE 迁移统一落库（JUDGING → 终态）
        submission.setScore(response.totalScore() == null ? 0 : response.totalScore());
        submission.setTimeUsed(maxTime);
        submission.setMemoryUsed(maxMemory);
        if (response.compile() != null && Boolean.FALSE.equals(response.compile().success())) {
            submission.setErrorMessage(StrUtil.subPre(response.compile().log(), 60000));
        }
        stateMachineService.sendEvent(submission, JudgeEvent.COMPLETE, finalStatus);
        if (!caseResults.isEmpty()) {
            // XML 批量插入不触发 MetaObjectHandler 与雪花 ID 填充，手动赋值
            for (JudgeCaseResult caseResult : caseResults) {
                caseResult.setId(IdWorker.getId());
            }
            judgeCaseResultMapper.insertBatch(caseResults);
        }
        log.info("提交 {} 判题完成 status={} score={}", submission.getSubmitId(), finalStatus, submission.getScore());
    }

    /**
     * 广播结果：JUDGE_RESULT_TOPIC + Redis 进度缓存 + WebSocket 推送
     */
    public void broadcast(JudgeSubmission submission) {
        JudgeStatusEnum status = JudgeStatusEnum.fromCode(submission.getStatus());

        // 1. 结果消息广播（题目统计/比赛成绩订阅）
        JudgeResultMessage resultMessage = JudgeResultMessage.builder()
                .submitId(submission.getSubmitId())
                .userId(submission.getUserId())
                .problemId(submission.getProblemId())
                .contestId(submission.getContestId())
                .statusCode(submission.getStatus())
                .score(submission.getScore())
                .timeUsed(submission.getTimeUsed())
                .memoryUsed(submission.getMemoryUsed())
                .judgeTime(submission.getJudgeTime())
                .build();
        rocketMQTemplate.syncSend(MqConstants.JUDGE_RESULT_TOPIC,
                MessageBuilder.withPayload(resultMessage)
                        .setHeader(RocketMQHeaders.KEYS, submission.getSubmitId())
                        .build(),
                5000);

        // 2. Redis 进度缓存（轮询兜底）
        JudgeProgressVO progress = SubmissionService.buildProgress(submission);
        try {
            stringRedisTemplate.opsForValue().set(RedisKeys.JUDGE_PROGRESS + submission.getSubmitId(),
                    objectMapper.writeValueAsString(progress), Duration.ofSeconds(progressTtlSeconds));
        } catch (Exception e) {
            log.warn("进度缓存写入失败 submitId={}", submission.getSubmitId(), e);
        }

        // 3. WebSocket 推送（本机 + 跨实例）
        judgeWsPushService.push(progress);
        log.info("提交 {} 结果已广播 status={}", submission.getSubmitId(), status);
    }

    /**
     * 执行失败处理：retry_count++ 落库，未达上限延时重投，否则抛异常走 RocketMQ 重试进 DLQ
     */
    private void handleExecuteFailure(JudgeSubmission submission, Exception e) {
        JudgeStatusEnum current = JudgeStatusEnum.fromCode(submission.getStatus());
        if (current.isTerminal()) {
            return;
        }
        submission.setRetryCount((submission.getRetryCount() == null ? 0 : submission.getRetryCount()) + 1);
        judgeSubmissionMapper.updateById(submission);

        if (submission.getRetryCount() < MqConstants.JUDGE_MAX_RETRY) {
            // 延时重新投递回原 topic
            JudgeSubmitMessage message = JudgeSubmitMessage.builder()
                    .submitId(submission.getSubmitId())
                    .submissionId(submission.getId())
                    .build();
            rocketMQTemplate.syncSend(MqConstants.JUDGE_SUBMIT_TOPIC,
                    MessageBuilder.withPayload(message)
                            .setHeader(RocketMQHeaders.KEYS, submission.getSubmitId())
                            .build(),
                    5000, RETRY_DELAY_LEVEL);
            log.info("提交 {} 判题失败，延时重投 retryCount={}", submission.getSubmitId(), submission.getRetryCount());
        } else {
            // 超过业务重试上限，抛异常让 RocketMQ 重试，最终进 DLQ
            throw e instanceof BizException be ? be : new BizException("判题失败：" + e.getMessage());
        }
    }
}
