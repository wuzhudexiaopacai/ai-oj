package com.github.wuzhudexiaopacai.oj.judge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.api.dto.JudgeCaseDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.ProblemJudgeDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.FileFeignClient;
import com.github.wuzhudexiaopacai.oj.api.feign.ProblemFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSandbox;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeCaseResultMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeOutboxMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxClient;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxLoadStrategy;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxRegistry;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.dto.SandboxJudgeRequest;
import com.github.wuzhudexiaopacai.oj.judge.statemachine.JudgeEvent;
import com.github.wuzhudexiaopacai.oj.judge.statemachine.JudgeStateMachineService;
import com.github.wuzhudexiaopacai.oj.judge.websocket.JudgeWsPushService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 判题执行测试：提交级互斥锁、沙箱选择与重试（4xx 不拉黑，5xx/网络异常换机）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class JudgeExecuteServiceTest {

    private static final Long SUBMISSION_ID = 1L;

    @Mock
    private JudgeSubmissionMapper judgeSubmissionMapper;

    @Mock
    private JudgeCaseResultMapper judgeCaseResultMapper;

    @Mock
    private JudgeOutboxMapper judgeOutboxMapper;

    @Mock
    private SandboxRegistry sandboxRegistry;

    @Mock
    private JudgeStateMachineService stateMachineService;

    @Mock
    private ProblemFeignClient problemFeignClient;

    @Mock
    private FileFeignClient fileFeignClient;

    @Mock
    private SandboxLoadStrategy sandboxLoadStrategy;

    @Mock
    private SandboxClient sandboxClient;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock executeLock;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private JudgeWsPushService judgeWsPushService;

    /**
     * 真实有界线程池（守护线程），验证用例文件并行拉取
     */
    @Spy
    private ExecutorService judgeRemoteReadExecutor = new ThreadPoolExecutor(
            2, 4, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100),
            runnable -> {
                Thread thread = new Thread(runnable, "judge-remote-read-test");
                thread.setDaemon(true);
                return thread;
            });

    @InjectMocks
    private JudgeExecuteService judgeExecuteService;

    @Test
    void clientErrorShouldNotBlacklistHealthySandbox() {
        JudgeSandbox sandbox = sandboxOf("sandbox-a");
        stubSubmissionFlow(List.of(sandbox, sandboxOf("sandbox-b")));
        when(sandboxLoadStrategy.select(anyList())).thenReturn(sandbox);
        when(sandboxClient.judge(anyString(), any()))
                .thenThrow(new WebClientResponseException(400, "Bad Request", HttpHeaders.EMPTY, null, null));

        assertDoesNotThrow(() -> judgeExecuteService.execute(SUBMISSION_ID));

        // 请求非法不换机、不拉黑，只调用一次
        verify(sandboxClient, times(1)).judge(anyString(), any());
        verify(sandboxLoadStrategy, never()).blacklist(anyString());
        verify(sandboxLoadStrategy, times(1)).release(sandbox.getSandboxKey());
    }

    @Test
    void busyShouldNotBlacklistSandbox() {
        JudgeSandbox sandbox = sandboxOf("sandbox-a");
        stubSubmissionFlow(List.of(sandbox));
        when(sandboxLoadStrategy.select(anyList())).thenReturn(sandbox);
        when(sandboxClient.judge(anyString(), any()))
                .thenThrow(new WebClientResponseException(503, "Service Unavailable", HttpHeaders.EMPTY, null, null));

        assertDoesNotThrow(() -> judgeExecuteService.execute(SUBMISSION_ID));

        // 繁忙不是故障：不拉黑、不换机，交给 MQ 延时重投
        verify(sandboxClient, times(1)).judge(anyString(), any());
        verify(sandboxLoadStrategy, never()).blacklist(anyString());
        verify(sandboxLoadStrategy, times(1)).release(sandbox.getSandboxKey());
    }

    @Test
    void serverErrorShouldBlacklistAndSwitchSandbox() {
        JudgeSandbox first = sandboxOf("sandbox-a");
        JudgeSandbox second = sandboxOf("sandbox-b");
        stubSubmissionFlow(List.of(first, second));
        when(sandboxLoadStrategy.select(anyList())).thenReturn(first, second, null);
        when(sandboxClient.judge(anyString(), any()))
                .thenThrow(new WebClientResponseException(500, "Internal Server Error", HttpHeaders.EMPTY, null, null));

        assertDoesNotThrow(() -> judgeExecuteService.execute(SUBMISSION_ID));

        verify(sandboxClient, times(2)).judge(anyString(), any());
        verify(sandboxLoadStrategy, times(2)).blacklist(anyString());
        verify(sandboxLoadStrategy, times(1)).blacklist(first.getSandboxKey());
        verify(sandboxLoadStrategy, times(1)).blacklist(second.getSandboxKey());
    }

    @Test
    void duplicatedConsumeShouldSkipWhenLockHeldByOthers() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(executeLock);
        when(executeLock.tryLock(anyLong(), any())).thenReturn(false);

        assertDoesNotThrow(() -> judgeExecuteService.execute(SUBMISSION_ID));

        verify(judgeSubmissionMapper, never()).selectById(anyLong());
        verifyNoInteractions(sandboxClient);
    }

    @Test
    void truncateUtf8ShouldRespectByteLimit() {
        String text = "编".repeat(30000);
        String truncated = JudgeExecuteService.truncateUtf8(text, 60000);

        assertTrue(truncated.getBytes(StandardCharsets.UTF_8).length <= 60000);
        assertFalse(truncated.contains("\uFFFD"), "截断不应留下半个多字节字符");
        assertEquals(20000, truncated.length());
        assertEquals("abc", JudgeExecuteService.truncateUtf8("abc", 100));
        assertNull(JudgeExecuteService.truncateUtf8(null, 10));
    }

    @Test
    void caseFilesShouldBeLoadedBeforeSandboxCall() {
        JudgeSandbox sandbox = sandboxOf("sandbox-a");
        stubSubmissionFlow(List.of(sandbox));
        ProblemJudgeDTO problem = problemWithOneCase();
        JudgeCaseDTO judgeCase = problem.getCases().get(0);
        // 内联内容为空、对象存储 key 非空：模拟大用例保存在 MinIO
        judgeCase.setInput(null);
        judgeCase.setInputFile("cases/1.in");
        judgeCase.setOutput(null);
        judgeCase.setOutputFile("cases/1.out");
        when(problemFeignClient.getJudgeInfo(anyLong())).thenReturn(Result.ok(problem));
        when(fileFeignClient.getContent("cases/1.in")).thenReturn(Result.ok("1 2\n"));
        when(fileFeignClient.getContent("cases/1.out")).thenReturn(Result.ok("3\n"));
        when(sandboxLoadStrategy.select(anyList())).thenReturn(sandbox);
        when(sandboxClient.judge(anyString(), any()))
                .thenThrow(new WebClientResponseException(400, "Bad Request", HttpHeaders.EMPTY, null, null));

        assertDoesNotThrow(() -> judgeExecuteService.execute(SUBMISSION_ID));

        ArgumentCaptor<SandboxJudgeRequest> captor = ArgumentCaptor.forClass(SandboxJudgeRequest.class);
        verify(sandboxClient).judge(anyString(), captor.capture());
        assertEquals("1 2\n", captor.getValue().cases().get(0).input());
        assertEquals("3\n", captor.getValue().cases().get(0).expectedOutput());
    }

    @Test
    void caseFileLoadFailureShouldAbortBeforeSandbox() {
        stubLockAndSubmission();
        ProblemJudgeDTO problem = problemWithOneCase();
        JudgeCaseDTO judgeCase = problem.getCases().get(0);
        judgeCase.setInput(null);
        judgeCase.setInputFile("cases/missing.in");
        when(problemFeignClient.getJudgeInfo(anyLong())).thenReturn(Result.ok(problem));
        when(fileFeignClient.getContent("cases/missing.in")).thenReturn(Result.failed("对象不存在"));
        when(judgeSubmissionMapper.updateById(any(JudgeSubmission.class))).thenReturn(1);

        assertDoesNotThrow(() -> judgeExecuteService.execute(SUBMISSION_ID));

        // 用例内容缺失不能进入评测，且失败按可重试处理（不落终态）
        verifyNoInteractions(sandboxClient);
        verify(judgeSubmissionMapper).updateById(any(JudgeSubmission.class));
        verify(stateMachineService, never()).sendEvent(any(), eq(JudgeEvent.COMPLETE), any());
    }

    private void stubSubmissionFlow(List<JudgeSandbox> sandboxes) {
        stubLockAndSubmission();
        when(sandboxRegistry.listAvailable()).thenReturn(sandboxes);
    }

    private void stubLockAndSubmission() {
        when(redissonClient.getLock(anyString())).thenReturn(executeLock);
        try {
            when(executeLock.tryLock(anyLong(), any())).thenReturn(true);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        when(executeLock.isHeldByCurrentThread()).thenReturn(true);
        when(judgeSubmissionMapper.selectById(anyLong())).thenReturn(pendingSubmission());
        when(problemFeignClient.getJudgeInfo(anyLong())).thenReturn(Result.ok(problemWithOneCase()));
    }

    private JudgeSubmission pendingSubmission() {
        JudgeSubmission submission = new JudgeSubmission();
        submission.setId(SUBMISSION_ID);
        submission.setSubmitId("submit-1");
        submission.setUserId(7L);
        submission.setProblemId(100L);
        submission.setLanguage("cpp");
        submission.setCode("int main() { return 0; }");
        submission.setStatus(0);
        return submission;
    }

    private ProblemJudgeDTO problemWithOneCase() {
        JudgeCaseDTO judgeCase = new JudgeCaseDTO();
        judgeCase.setSeq(1);
        judgeCase.setInput("1 2\n");
        judgeCase.setOutput("3\n");
        judgeCase.setScore(100);
        ProblemJudgeDTO problem = new ProblemJudgeDTO();
        problem.setProblemId(100L);
        problem.setTimeLimit(1000);
        problem.setMemoryLimit(256);
        problem.setCases(List.of(judgeCase));
        return problem;
    }

    private JudgeSandbox sandboxOf(String key) {
        JudgeSandbox sandbox = new JudgeSandbox();
        sandbox.setSandboxKey(key);
        sandbox.setBaseUrl("http://" + key + ":8000");
        sandbox.setMaxParallel(2);
        sandbox.setStatus(1);
        return sandbox;
    }
}
