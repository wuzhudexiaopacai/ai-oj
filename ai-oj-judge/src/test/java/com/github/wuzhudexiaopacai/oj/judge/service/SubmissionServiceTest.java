package com.github.wuzhudexiaopacai.oj.judge.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.wuzhudexiaopacai.oj.api.dto.ProblemBriefDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.FileFeignClient;
import com.github.wuzhudexiaopacai.oj.api.feign.ProblemFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeCaseResult;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeCaseResultMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import com.github.wuzhudexiaopacai.oj.judge.vo.SubmissionDetailVO;
import com.github.wuzhudexiaopacai.oj.judge.vo.SubmissionPageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SubmissionService 提交详情单元测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private JudgeSubmissionMapper judgeSubmissionMapper;

    @Mock
    private JudgeCaseResultMapper judgeCaseResultMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProblemFeignClient problemFeignClient;

    @Mock
    private FileFeignClient fileFeignClient;

    /**
     * 真实有界线程池（守护线程），验证题目简要信息并行回填
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
    private SubmissionService submissionService;

    @BeforeEach
    void login() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(LoginUser.builder().id(7L).build(), null));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 构造一条已通过的提交记录
     */
    private JudgeSubmission submissionOf(Long id, Long userId, int codeLength) {
        JudgeSubmission submission = new JudgeSubmission();
        submission.setId(id);
        submission.setUserId(userId);
        submission.setLanguage("java");
        submission.setCode("public class Main {}");
        submission.setCodeLength(codeLength);
        submission.setStatus(JudgeStatusEnum.ACCEPTED.getCode());
        submission.setScore(100);
        return submission;
    }

    @Test
    void detailReturnsCodeLengthAndCode() {
        when(judgeSubmissionMapper.selectById(1L)).thenReturn(submissionOf(1L, 7L, 1234));
        lenient().when(judgeCaseResultMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        SubmissionDetailVO vo = submissionService.detail(1L);

        assertEquals(1234, vo.getCodeLength());
        assertEquals("public class Main {}", vo.getCode());
        assertEquals(JudgeStatusEnum.ACCEPTED.getDesc(), vo.getStatusDesc());
    }

    @Test
    void detailReturnsCaseItems() {
        when(judgeSubmissionMapper.selectById(2L)).thenReturn(submissionOf(2L, 7L, 40));
        JudgeCaseResult caseResult = new JudgeCaseResult();
        caseResult.setSeq(1);
        caseResult.setStatus(JudgeStatusEnum.ACCEPTED.getCode());
        caseResult.setTimeUsed(10);
        caseResult.setMemoryUsed(1024);
        caseResult.setScore(100);
        when(judgeCaseResultMapper.selectList(any(Wrapper.class))).thenReturn(List.of(caseResult));

        SubmissionDetailVO vo = submissionService.detail(2L);

        assertEquals(1, vo.getCases().size());
        assertEquals(JudgeStatusEnum.ACCEPTED.getDesc(), vo.getCases().get(0).getStatusDesc());
    }

    @Test
    void detailRejectsOtherUsersSubmission() {
        when(judgeSubmissionMapper.selectById(3L)).thenReturn(submissionOf(3L, 8L, 40));

        assertThrows(BizException.class, () -> submissionService.detail(3L));
    }

    @Test
    void pageFillsProblemBriefsInParallelWithoutDuplicateFetch() {
        JudgeSubmission first = submissionOf(1L, 7L, 40);
        first.setSubmitId("submit-1");
        first.setProblemId(100L);
        JudgeSubmission second = submissionOf(2L, 7L, 40);
        second.setSubmitId("submit-2");
        second.setProblemId(100L);
        Page<JudgeSubmission> page = new Page<>(1, 10);
        page.setRecords(List.of(first, second));
        page.setTotal(2);
        when(judgeSubmissionMapper.selectPage(any(Page.class), any())).thenReturn(page);
        ProblemBriefDTO brief = new ProblemBriefDTO();
        brief.setProblemId(100L);
        brief.setProblemKey("A1001");
        brief.setTitle("两数之和");
        when(problemFeignClient.getBrief(100L)).thenReturn(Result.ok(brief));

        PageResult<SubmissionPageVO> result = submissionService.page(1, 10, null, null, null, null, null, null);

        assertEquals(2, result.getRecords().size());
        assertEquals("A1001", result.getRecords().get(0).getProblemKey());
        assertEquals("两数之和", result.getRecords().get(1).getProblemTitle());
        // 同一题目在本页内只远程拉取一次
        verify(problemFeignClient, times(1)).getBrief(100L);
    }

    @Test
    void pageDegradesWhenProblemBriefFetchFails() {
        JudgeSubmission submission = submissionOf(4L, 7L, 40);
        submission.setSubmitId("submit-4");
        submission.setProblemId(200L);
        Page<JudgeSubmission> page = new Page<>(1, 10);
        page.setRecords(List.of(submission));
        page.setTotal(1);
        when(judgeSubmissionMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(problemFeignClient.getBrief(200L)).thenThrow(new RuntimeException("题目服务不可用"));

        PageResult<SubmissionPageVO> result = assertDoesNotThrow(
                () -> submissionService.page(1, 10, null, null, null, null, null, null));

        // 回填失败只降级，不影响分页主体
        assertEquals(1, result.getRecords().size());
        assertNull(result.getRecords().get(0).getProblemKey());
        assertNull(result.getRecords().get(0).getProblemTitle());
    }
}
