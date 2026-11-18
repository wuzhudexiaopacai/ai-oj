package com.github.wuzhudexiaopacai.oj.judge.service;

import com.github.wuzhudexiaopacai.oj.api.dto.ProblemBriefDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.ContestFeignClient;
import com.github.wuzhudexiaopacai.oj.api.feign.ProblemFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.judge.dto.JudgeSubmitRequest;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxClient;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxLoadStrategy;
import com.github.wuzhudexiaopacai.oj.judge.sandbox.SandboxRegistry;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    private JudgeSubmitRequest requestOf(String language) {
        JudgeSubmitRequest req = new JudgeSubmitRequest();
        req.setSubmitId("submit-1");
        req.setProblemId(1L);
        req.setLanguage(language);
        req.setCode("int main() { return 0; }");
        return req;
    }
}
