package com.github.wuzhudexiaopacai.oj.judge.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.github.wuzhudexiaopacai.oj.api.dto.ProblemBriefDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.ContestFeignClient;
import com.github.wuzhudexiaopacai.oj.api.feign.ProblemFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.judge.dto.CaseValidateRequest;
import com.github.wuzhudexiaopacai.oj.judge.dto.JudgeRunRequest;
import com.github.wuzhudexiaopacai.oj.judge.dto.JudgeSubmitRequest;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSandbox;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeCaseResultMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeOutboxMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxClient;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxLoadStrategy;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxRegistry;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.dto.SandboxRunRequest;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.dto.SandboxRunResponse;
import com.github.wuzhudexiaopacai.oj.judge.vo.CaseValidateVO;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.messaging.Message;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 判题提交前置校验测试：语言白名单、题目存在性与下架状态
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class JudgeServiceTest {

    @Mock
    private JudgeSubmissionMapper judgeSubmissionMapper;

    @Mock
    private JudgeCaseResultMapper judgeCaseResultMapper;

    @Mock
    private JudgeOutboxMapper judgeOutboxMapper;

    @Mock
    private SandboxRegistry sandboxRegistry;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private SandboxLoadStrategy sandboxLoadStrategy;

    @Mock
    private SandboxClient sandboxClient;

    @Mock
    private ContestFeignClient contestFeignClient;

    @Mock
    private ProblemFeignClient problemFeignClient;

    @Mock
    private RRateLimiter rateLimiter;

    @InjectMocks
    private JudgeService judgeService;

    @BeforeEach
    void setUp() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(LoginUser.builder().id(7L).build(), null));
        Field field = JudgeService.class.getDeclaredField("supportedLanguages");
        field.setAccessible(true);
        field.set(judgeService, List.of("c", "cpp", "java", "python3", "go", "javascript", "rust"));
    }

    /**
     * LambdaUpdateWrapper 需要实体 TableInfo 缓存，纯单测环境手动注册
     */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                JudgeSubmission.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitShouldRejectUnsupportedLanguage() {
        JudgeSubmitRequest req = requestOf("brainfuck");

        BizException ex = assertThrows(BizException.class, () -> judgeService.submit(req));

        assertTrue(ex.getMessage().contains("不支持的编程语言"));
        verifyNoInteractions(problemFeignClient);
    }

    @Test
    void submitShouldRejectOfflineProblem() {
        JudgeSubmitRequest req = requestOf("cpp");
        ProblemBriefDTO brief = new ProblemBriefDTO();
        brief.setStatus(0);
        when(problemFeignClient.getBrief(1L)).thenReturn(Result.ok(brief));

        BizException ex = assertThrows(BizException.class, () -> judgeService.submit(req));

        assertTrue(ex.getMessage().contains("已下架"));
    }

    @Test
    void submitShouldRejectMissingProblem() {
        JudgeSubmitRequest req = requestOf("cpp");
        when(problemFeignClient.getBrief(1L)).thenReturn(Result.ok(null));

        BizException ex = assertThrows(BizException.class, () -> judgeService.submit(req));

        assertTrue(ex.getMessage().contains("题目不存在"));
    }

    @Test
    void submitShouldRejectWhenProblemServiceUnavailable() {
        JudgeSubmitRequest req = requestOf("cpp");
        when(problemFeignClient.getBrief(1L)).thenThrow(new RuntimeException("boom"));

        BizException ex = assertThrows(BizException.class, () -> judgeService.submit(req));

        assertTrue(ex.getMessage().contains("暂不可用"));
    }

    @Test
    void rejudgeShouldResetResultAndRequeue() {
        JudgeSubmission submission = new JudgeSubmission();
        submission.setId(1L);
        submission.setSubmitId("submit-1");
        submission.setStatus(JudgeStatusEnum.WRONG_ANSWER.getCode());
        when(judgeSubmissionMapper.selectById(1L)).thenReturn(submission);

        judgeService.rejudge(1L);

        verify(judgeSubmissionMapper).update(isNull(), any());
        verify(judgeCaseResultMapper).delete(any());
        verify(judgeOutboxMapper).delete(any());
        verify(rocketMQTemplate).syncSend(eq(MqConstants.JUDGE_SUBMIT_TOPIC), any(Message.class), eq(5000L));
    }

    @Test
    void rejudgeShouldRejectRunningSubmission() {
        JudgeSubmission submission = new JudgeSubmission();
        submission.setId(1L);
        submission.setSubmitId("submit-1");
        submission.setStatus(JudgeStatusEnum.JUDGING.getCode());
        when(judgeSubmissionMapper.selectById(1L)).thenReturn(submission);

        BizException ex = assertThrows(BizException.class, () -> judgeService.rejudge(1L));

        assertTrue(ex.getMessage().contains("仍在判题中"));
        verify(judgeSubmissionMapper, never()).update(any(), any());
        verifyNoInteractions(rocketMQTemplate);
    }

    @Test
    void rejudgeShouldRejectMissingSubmission() {
        when(judgeSubmissionMapper.selectById(1L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> judgeService.rejudge(1L));

        assertTrue(ex.getMessage().contains("提交记录不存在"));
        verifyNoInteractions(rocketMQTemplate);
    }

    @Test
    void runShouldRejectUnsupportedLanguageBeforeSandbox() {
        givenRateLimiterAcquires();

        BizException ex = assertThrows(BizException.class,
                () -> judgeService.run(runRequestOf("brainfuck")));

        assertTrue(ex.getMessage().contains("不支持的编程语言"));
        verifyNoInteractions(sandboxRegistry, sandboxClient, sandboxLoadStrategy);
    }

    @Test
    void runShouldNotBlacklistSandboxOnClientError() {
        givenRateLimiterAcquires();
        givenSingleSandbox();
        when(sandboxClient.run(anyString(), any()))
                .thenThrow(sandboxError(400, "Bad Request"));

        assertThrows(BizException.class, () -> judgeService.run(runRequestOf("cpp")));

        verify(sandboxLoadStrategy).release("sbx-1");
        verify(sandboxLoadStrategy, never()).blacklist(anyString());
    }

    @Test
    void runShouldBlacklistSandboxOnServerError() {
        givenRateLimiterAcquires();
        givenSingleSandbox();
        when(sandboxClient.run(anyString(), any()))
                .thenThrow(sandboxError(500, "Internal Server Error"));

        assertThrows(BizException.class, () -> judgeService.run(runRequestOf("cpp")));

        verify(sandboxLoadStrategy).blacklist("sbx-1");
        verify(sandboxLoadStrategy).release("sbx-1");
    }

    @Test
    void validateCasesShouldClampTimeLimitAndNotBlacklistOnClientError() {
        givenRateLimiterAcquires();
        givenSingleSandbox();
        when(sandboxClient.run(anyString(), any()))
                .thenThrow(sandboxError(400, "Bad Request"));

        CaseValidateRequest req = new CaseValidateRequest();
        req.setLanguage("cpp");
        req.setCode("int main() { return 0; }");
        req.setTimeLimitMs(999999);
        CaseValidateRequest.CaseItem item = new CaseValidateRequest.CaseItem();
        item.setSeq(1);
        item.setExpectedOutput("3");
        req.setCases(List.of(item));

        assertThrows(BizException.class, () -> judgeService.validateCases(req));

        ArgumentCaptor<SandboxRunRequest> captor = ArgumentCaptor.forClass(SandboxRunRequest.class);
        verify(sandboxClient).run(anyString(), captor.capture());
        assertEquals(30000, captor.getValue().timeLimitMs().intValue());
        verify(sandboxLoadStrategy, never()).blacklist(anyString());
        verify(sandboxLoadStrategy).release("sbx-1");
    }

    @Test
    void validateCasesShouldNotPassWhenReferenceRunFailsButOutputMatches() {
        givenRateLimiterAcquires();
        givenSingleSandbox();
        when(sandboxClient.run(anyString(), any()))
                .thenReturn(new SandboxRunResponse("TIME_LIMIT_EXCEEDED", "3", "", 10000, 1024));

        CaseValidateVO vo = judgeService.validateCases(validateRequestOf("3"));

        assertFalse(vo.getCases().get(0).getPassed());
        assertEquals(0, vo.getPassedCount().intValue());
        assertFalse(vo.getOverallPassed());
    }

    @Test
    void validateCasesShouldPassWhenReferenceRunAcceptedAndOutputMatches() {
        givenRateLimiterAcquires();
        givenSingleSandbox();
        when(sandboxClient.run(anyString(), any()))
                .thenReturn(new SandboxRunResponse("ACCEPTED", "3\n", "", 10, 1024));

        CaseValidateVO vo = judgeService.validateCases(validateRequestOf("3"));

        assertTrue(vo.getCases().get(0).getPassed());
        assertEquals(1, vo.getPassedCount().intValue());
        assertTrue(vo.getOverallPassed());
    }

    private CaseValidateRequest validateRequestOf(String expectedOutput) {
        CaseValidateRequest req = new CaseValidateRequest();
        req.setLanguage("cpp");
        req.setCode("int main() { return 0; }");
        CaseValidateRequest.CaseItem item = new CaseValidateRequest.CaseItem();
        item.setSeq(1);
        item.setExpectedOutput(expectedOutput);
        req.setCases(List.of(item));
        return req;
    }

    private void givenRateLimiterAcquires() {
        when(redissonClient.getRateLimiter(anyString())).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire()).thenReturn(true);
    }

    private void givenSingleSandbox() {
        JudgeSandbox sandbox = new JudgeSandbox();
        sandbox.setSandboxKey("sbx-1");
        sandbox.setBaseUrl("http://sandbox:8080");
        when(sandboxRegistry.listAvailable()).thenReturn(List.of(sandbox));
        when(sandboxLoadStrategy.select(anyList())).thenReturn(sandbox);
    }

    private WebClientResponseException sandboxError(int status, String reason) {
        return WebClientResponseException.create(status, reason, HttpHeaders.EMPTY, new byte[0],
                StandardCharsets.UTF_8);
    }

    private JudgeRunRequest runRequestOf(String language) {
        JudgeRunRequest req = new JudgeRunRequest();
        req.setLanguage(language);
        req.setCode("print(1)");
        return req;
    }

    private JudgeSubmitRequest requestOf(String language) {
        JudgeSubmitRequest req = new JudgeSubmitRequest();
        req.setSubmitId("submit-1");
        req.setProblemId(1L);
        req.setLanguage(language);
        req.setCode("int main() { return 0; }");
        return req;
    }
}
