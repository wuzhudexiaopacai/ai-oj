package com.github.wuzhudexiaopacai.oj.judge.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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
     * 重投递延时级别（RocketMQ 延时级别 3 = 10s）
     */
    private static final int RETRY_DELAY_LEVEL = 3;

    /**
     * 题目未配置 stack_limit 时的默认栈空间（MB）
     */
    private static final int DEFAULT_STACK_LIMIT_MB = 256;

    /**
     * error_message 最大字节数：MySQL TEXT 上限 65535 字节，
     * 中文 3 字节/字，按字符截断仍可能溢出，必须按 UTF-8 字节截断
     */
    private static final int ERROR_MESSAGE_MAX_BYTES = 60000;

    private final JudgeSubmissionMapper judgeSubmissionMapper;

    private final JudgeCaseResultMapper judgeCaseResultMapper;

    private final JudgeOutboxMapper judgeOutboxMapper;

    private final SandboxRegistry sandboxRegistry;

    private final JudgeStateMachineService stateMachineService;

    private final ProblemFeignClient problemFeignClient;

    private final FileFeignClient fileFeignClient;

    /**
     * 远程读并行执行器（有界线程池），用于并行拉取用例文件
     */
    private final ExecutorService judgeRemoteReadExecutor;

    private final SandboxLoadStrategy sandboxLoadStrategy;

    private final SandboxClient sandboxClient;

    private final RocketMQTemplate rocketMQTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    private final RedissonClient redissonClient;

    private final ObjectMapper objectMapper;

    private final JudgeWsPushService judgeWsPushService;

    /**
     * 编程式事务：用于把「用例明细 + 终态 + 结果 Outbox」放进同一个本地事务
     */
    private final TransactionTemplate transactionTemplate;

    @Value("${ai-oj.judge.progress-ttl-seconds:3600}")
    private long progressTtlSeconds;

    /**
     * 执行判题主流程
     * <p>
     * 同一提交可能被 MQ 重投与卡死补偿任务同时消费，必须按 submissionId 互斥：
     * 抢不到锁说明已有实例在判，本次直接返回，由持锁实例完成落库与广播。
     *
     * @param submissionId 提交记录 ID
     */
    public void execute(Long submissionId) {
        RLock lock = redissonClient.getLock(RedisKeys.JUDGE_EXECUTE_LOCK + submissionId);
        boolean locked;
        try {
            locked = lock.tryLock(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("提交 {} 获取判题锁被中断，跳过本次消费", submissionId);
            return;
        }
        if (!locked) {
            log.info("提交 {} 正在判题中，跳过本次重复消费", submissionId);
            return;
        }
        try {
            doExecute(submissionId);
        } finally {
            // 只释放本线程持有的锁，避免误删其他实例的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 判题执行主体（调用方已持有提交级互斥锁）
     *
     * @param submissionId 提交记录 ID
     */
    private void doExecute(Long submissionId) {
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
     * 拉取题目判题信息
     * <p>
     * 不使用 Redis 缓存：该对象包含全部评测用例与 SPJ 源码，明文写入共享 Redis
     * 会让任何能访问 Redis 的组件直接拿到测试数据。每次判题按需经 Feign/MinIO 拉取。
     */
    private ProblemJudgeDTO loadProblemInfo(Long problemId) {
        Result<ProblemJudgeDTO> result = problemFeignClient.getJudgeInfo(problemId);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BizException("拉取题目判题信息失败 problemId=" + problemId);
        }
        ProblemJudgeDTO problem = result.getData();

        // 用例内容补全：input 为空且 inputFile 非空 → 从 MinIO 拉取（output 同理）
        fillCaseContent(problem.getCases());
        return problem;
    }

    /**
     * 并行补全用例输入/输出内容
     * <p>
     * 用例文件全部存放在 MinIO，串行拉取会让判题启动耗时随用例数线性增长；
     * 每个文件相互独立，使用有界线程池并行拉取。任一失败即整题失败，
     * 并保持原异常语义（业务异常原样抛出，其余运行时异常原样上抛）。
     *
     * @param cases 评测用例
     */
    private void fillCaseContent(List<JudgeCaseDTO> cases) {
        if (cases == null || cases.isEmpty()) {
            return;
        }
        List<CompletableFuture<Void>> futures = new ArrayList<>(cases.size() * 2);
        for (JudgeCaseDTO judgeCase : cases) {
            if (StrUtil.isBlank(judgeCase.getInput()) && StrUtil.isNotBlank(judgeCase.getInputFile())) {
                String objectKey = judgeCase.getInputFile();
                futures.add(CompletableFuture
                        .supplyAsync(() -> loadFileContent(objectKey), judgeRemoteReadExecutor)
                        .thenAccept(judgeCase::setInput));
            }
            if (StrUtil.isBlank(judgeCase.getOutput()) && StrUtil.isNotBlank(judgeCase.getOutputFile())) {
                String objectKey = judgeCase.getOutputFile();
                futures.add(CompletableFuture
                        .supplyAsync(() -> loadFileContent(objectKey), judgeRemoteReadExecutor)
                        .thenAccept(judgeCase::setOutput));
            }
        }
        if (futures.isEmpty()) {
            return;
        }
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new BizException("拉取用例文件失败：" + cause.getMessage());
        }
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
            } catch (WebClientResponseException e) {
                lastException = e;
                int status = e.getStatusCode().value();
                // 429/503 表示沙箱繁忙（槽位排队超时），实例本身健康：不拉黑，交由 MQ 延时重投
                if (status == 429 || status == 503) {
                    log.warn("沙箱繁忙 HTTP {}，不拉黑，稍后重试 sandbox={}", status, sandbox.getSandboxKey());
                    throw new BizException("沙箱繁忙：HTTP " + status);
                }
                // 其余 4xx 表示请求本身不合法（参数/用例/令牌错误），换沙箱重试无意义，
                // 且沙箱本身是健康的，不能拉黑，否则一次坏请求会打掉正常实例
                if (status >= 400 && status < 500) {
                    log.error("沙箱拒绝判题请求 HTTP {}，请求非法不重试 sandbox={}", status, sandbox.getSandboxKey(), e);
                    throw new BizException("沙箱拒绝判题请求：HTTP " + status);
                }
                log.error("沙箱调用失败，拉黑并换机重试 sandbox={} attempt={}", sandbox.getSandboxKey(), i + 1, e);
                sandboxLoadStrategy.blacklist(sandbox.getSandboxKey());
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
            submission.setErrorMessage(truncateUtf8(response.compile().log(), ERROR_MESSAGE_MAX_BYTES));
        }

        // 用例明细、终态、结果消息同事务落库：
        // 1) 先写明细、后迁移终态：终态一旦落库就不会再重试，明细失败则整体回滚，提交停在 JUDGING 被重试，
        //    避免“有结论但用例明细永久缺失”；重试时先删后插，保证明细不重复；
        // 2) 结果消息（Outbox）与终态在同一事务提交：进程若在“终态提交成功、消息尚未投递”之间崩溃，
        //    补偿任务仍能从 Outbox 补投，不会永久丢失题目统计/比赛成绩。
        transactionTemplate.executeWithoutResult(status -> {
            judgeCaseResultMapper.delete(new LambdaQueryWrapper<JudgeCaseResult>()
                    .eq(JudgeCaseResult::getSubmissionId, submission.getId()));
            if (!caseResults.isEmpty()) {
                // XML 批量插入不触发 MetaObjectHandler 与雪花 ID 填充，手动赋值
                for (JudgeCaseResult caseResult : caseResults) {
                    caseResult.setId(IdWorker.getId());
                }
                judgeCaseResultMapper.insertBatch(caseResults);
            }
            stateMachineService.sendEvent(submission, JudgeEvent.COMPLETE, finalStatus);
            try {
                createOutboxIfAbsent(submission);
            } catch (Exception e) {
                // 结果消息落库失败不阻断终态提交（否则会回滚已完成的判题），退化为广播时重写或直接投递
                log.error("结果消息落本地消息表失败，广播时重试 submitId={}", submission.getSubmitId(), e);
            }
        });
        log.info("提交 {} 判题完成 status={} score={}", submission.getSubmitId(), finalStatus, submission.getScore());
    }

    /**
     * 广播结果：JUDGE_RESULT_TOPIC + Redis 进度缓存 + WebSocket 推送
     */
    public void broadcast(JudgeSubmission submission) {
        JudgeStatusEnum status = JudgeStatusEnum.fromCode(submission.getStatus());

        // 1. 结果消息广播（题目统计/比赛成绩订阅）
        JudgeResultMessage resultMessage = buildResultMessage(submission);
        // 结果消息先落本地消息表（Outbox），再尝试投递：
        // 正常判题链路已在终态事务内写入 Outbox，此处复用同一条记录；
        // 投递失败保留待投递状态，由 ResultOutboxResendJob 退避补投，避免结果永久丢失
        JudgeOutbox outbox = saveOutbox(submission);
        if (outbox == null || !Integer.valueOf(JudgeOutbox.STATUS_SENT).equals(outbox.getStatus())) {
            deliverResult(outbox, resultMessage);
        }

        // 2. Redis 进度缓存（轮询兜底）
        JudgeProgressVO progress = SubmissionService.buildProgress(submission);
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisKeys.JUDGE_PROGRESS + submission.getUserId() + ":" + submission.getSubmitId(),
                    objectMapper.writeValueAsString(progress), Duration.ofSeconds(progressTtlSeconds));
        } catch (Exception e) {
            log.warn("进度缓存写入失败 submitId={}", submission.getSubmitId(), e);
        }

        // 3. WebSocket 推送（本机 + 跨实例）
        judgeWsPushService.push(progress);
        log.info("提交 {} 结果已广播 status={}", submission.getSubmitId(), status);
    }

    /**
     * 组装结果广播消息
     */
    private JudgeResultMessage buildResultMessage(JudgeSubmission submission) {
        return JudgeResultMessage.builder()
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
    }

    /**
     * 结果消息落本地消息表；已存在（重复广播）时直接复用原记录
     *
     * @return Outbox 记录，落库失败返回 null（调用方退化为直接投递）
     */
    private JudgeOutbox saveOutbox(JudgeSubmission submission) {
        try {
            return createOutboxIfAbsent(submission);
        } catch (Exception e) {
            log.error("结果消息落本地消息表失败，退化为直接投递 submitId={}", submission.getSubmitId(), e);
            return null;
        }
    }

    /**
     * 按 submitId 幂等写入 Outbox（存在则复用），供终态事务内调用
     *
     * @param submission 提交记录（终态已落库，包含 judgeTime）
     * @return 已存在或新写入的 Outbox 记录
     */
    private JudgeOutbox createOutboxIfAbsent(JudgeSubmission submission) throws Exception {
        JudgeOutbox existing = judgeOutboxMapper.selectOne(new LambdaQueryWrapper<JudgeOutbox>()
                .eq(JudgeOutbox::getSubmitId, submission.getSubmitId())
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        JudgeOutbox outbox = new JudgeOutbox();
        outbox.setSubmitId(submission.getSubmitId());
        outbox.setPayload(objectMapper.writeValueAsString(buildResultMessage(submission)));
        outbox.setStatus(JudgeOutbox.STATUS_PENDING);
        outbox.setRetryCount(0);
        outbox.setNextRetryTime(LocalDateTime.now());
        judgeOutboxMapper.insert(outbox);
        return outbox;
    }

    /**
     * 兜底置终态并把结果消息写入 Outbox（同一事务）
     * <p>
     * 死信/卡死补偿链路使用：终态与待投递消息原子落库，避免“已置终态但结果未广播”的丢失窗口。
     * 返回 true 后调用方再执行 {@link #broadcast(JudgeSubmission)}，会复用已写入的 Outbox 记录投递。
     *
     * @param submission     提交记录
     * @param terminalStatus 目标终态
     * @param errorMessage   错误信息
     * @return 仅本次成功置为终态返回 true；已终态或并发冲突返回 false
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean transitionToTerminalAndSaveOutbox(JudgeSubmission submission, JudgeStatusEnum terminalStatus,
                                                     String errorMessage) {
        if (!stateMachineService.transitionToTerminal(submission, terminalStatus, errorMessage)) {
            return false;
        }
        try {
            createOutboxIfAbsent(submission);
        } catch (Exception e) {
            // 落库失败不阻断终态，广播时会重试写入或退化为直接投递
            log.error("兜底终态结果落本地消息表失败，广播时重试 submitId={}", submission.getSubmitId(), e);
        }
        return true;
    }

    /**
     * 投递结果消息；成功后标记 Outbox 已投递，失败则保留待投递由补偿任务重试
     */
    private void deliverResult(JudgeOutbox outbox, JudgeResultMessage resultMessage) {
        try {
            rocketMQTemplate.syncSend(MqConstants.JUDGE_RESULT_TOPIC,
                    MessageBuilder.withPayload(resultMessage)
                            .setHeader(RocketMQHeaders.KEYS, resultMessage.getSubmitId())
                            .build(),
                    5000);
        } catch (Exception e) {
            log.warn("结果消息投递失败，等待补偿任务补投 submitId={}", resultMessage.getSubmitId(), e);
            return;
        }
        if (outbox == null) {
            return;
        }
        try {
            outbox.setStatus(JudgeOutbox.STATUS_SENT);
            judgeOutboxMapper.updateById(outbox);
        } catch (Exception e) {
            // 消息已发出但状态未更新：补偿任务会重复投递一次，下游按 submitId 幂等
            log.warn("结果消息已投递但 Outbox 状态更新失败 submitId={}", resultMessage.getSubmitId(), e);
        }
    }

    /**
     * 按 UTF-8 字节截断文本，避免超过 TEXT 列上限导致落库失败
     * <p>
     * 截断点可能落在多字节字符中间，需要去掉末尾残生的替换字符。
     *
     * @param text     原始文本
     * @param maxBytes 最大字节数
     * @return 截断后的文本
     */
    static String truncateUtf8(String text, int maxBytes) {
        if (text == null) {
            return null;
        }
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return text;
        }
        String truncated = new String(bytes, 0, maxBytes, StandardCharsets.UTF_8);
        int end = truncated.length();
        while (end > 0 && truncated.charAt(end - 1) == '\uFFFD') {
            end--;
        }
        return truncated.substring(0, end);
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
        // 乐观锁冲突（影响行数为 0）说明其他实例已更新该提交，放弃本次重试避免重复判题
        if (judgeSubmissionMapper.updateById(submission) == 0) {
            log.warn("提交 {} 状态已被并发修改，跳过本次失败重试", submission.getSubmitId());
            return;
        }

        if (submission.getRetryCount() < MqConstants.JUDGE_MAX_RETRY) {
            // 延时重新投递回原 topic；随重试次数递增延时级别（10s -> 30s -> 1m），
            // 沙箱持续繁忙时给队列留出消化时间，避免秒级反复抢占
            int delayLevel = RETRY_DELAY_LEVEL + Math.min(submission.getRetryCount(), 2);
            JudgeSubmitMessage message = JudgeSubmitMessage.builder()
                    .submitId(submission.getSubmitId())
                    .submissionId(submission.getId())
                    .build();
            rocketMQTemplate.syncSend(MqConstants.JUDGE_SUBMIT_TOPIC,
                    MessageBuilder.withPayload(message)
                            .setHeader(RocketMQHeaders.KEYS, submission.getSubmitId())
                            .build(),
                    5000, delayLevel);
            log.info("提交 {} 判题失败，延时重投 retryCount={} delayLevel={}",
                    submission.getSubmitId(), submission.getRetryCount(), delayLevel);
        } else {
            // 超过业务重试上限，抛异常让 RocketMQ 重试，最终进 DLQ
            throw e instanceof BizException be ? be : new BizException("判题失败：" + e.getMessage());
        }
    }
}
