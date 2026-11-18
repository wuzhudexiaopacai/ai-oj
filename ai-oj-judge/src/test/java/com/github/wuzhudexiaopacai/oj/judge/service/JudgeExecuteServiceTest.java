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
import com.github.wuzhudexiaopacai.oj.judge.statemachine.JudgeStateMachineService;
import com.github.wuzhudexiaopacai.oj.judge.websocket.JudgeWsPushService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

    private void stubSubmissionFlow(List<JudgeSandbox> sandboxes) {
        when(redissonClient.getLock(anyString())).thenReturn(executeLock);
        try {
            when(executeLock.tryLock(anyLong(), any())).thenReturn(true);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        when(executeLock.isHeldByCurrentThread()).thenReturn(true);
        when(judgeSubmissionMapper.selectById(anyLong())).thenReturn(pendingSubmission());
        when(problemFeignClient.getJudgeInfo(anyLong())).thenReturn(Result.ok(problemWithOneCase()));
        when(sandboxRegistry.listAvailable()).thenReturn(sandboxes);
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
