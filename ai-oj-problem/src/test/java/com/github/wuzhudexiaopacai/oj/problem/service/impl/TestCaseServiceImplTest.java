package com.github.wuzhudexiaopacai.oj.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.problem.dto.TestCaseSaveDTO;
import com.github.wuzhudexiaopacai.oj.problem.entity.Problem;
import com.github.wuzhudexiaopacai.oj.problem.entity.TestCase;
import com.github.wuzhudexiaopacai.oj.problem.mapper.ProblemMapper;
import com.github.wuzhudexiaopacai.oj.problem.mapper.TestCaseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 评测用例管理服务测试：seq 唯一性与上架题目最后一条用例保护
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class TestCaseServiceImplTest {

    @Mock
    private TestCaseMapper testCaseMapper;

    @Mock
    private ProblemMapper problemMapper;

    @InjectMocks
    private TestCaseServiceImpl testCaseService;

    @Test
    void createShouldRejectDuplicatedSeq() {
        when(problemMapper.selectById(1L)).thenReturn(problemOf(1));
        when(testCaseMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> testCaseService.create(1L, caseOf(2)));

        assertTrue(ex.getMessage().contains("用例序号已存在"));
    }

    @Test
    void createShouldRejectNonPositiveSeq() {
        when(problemMapper.selectById(1L)).thenReturn(problemOf(1));

        BizException ex = assertThrows(BizException.class, () -> testCaseService.create(1L, caseOf(0)));

        assertTrue(ex.getMessage().contains("必须为正整数"));
        verify(testCaseMapper, never()).insert(any(TestCase.class));
    }

    @Test
    void deleteShouldRejectLastCaseOfOnlineProblem() {
        when(problemMapper.selectById(1L)).thenReturn(problemOf(1));
        when(testCaseMapper.selectById(9L)).thenReturn(caseEntityOf(9L, 1L));
        when(testCaseMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        BizException ex = assertThrows(BizException.class, () -> testCaseService.delete(1L, 9L));

        assertTrue(ex.getMessage().contains("至少需要保留"));
        verify(testCaseMapper, never()).deleteById(9L);
    }

    @Test
    void deleteShouldAllowLastCaseOfDraftProblem() {
        when(problemMapper.selectById(1L)).thenReturn(problemOf(0));
        when(testCaseMapper.selectById(9L)).thenReturn(caseEntityOf(9L, 1L));

        testCaseService.delete(1L, 9L);

        verify(testCaseMapper).deleteById(9L);
    }

    private Problem problemOf(int status) {
        Problem problem = new Problem();
        problem.setId(1L);
        problem.setStatus(status);
        return problem;
    }

    private TestCaseSaveDTO caseOf(int seq) {
        TestCaseSaveDTO dto = new TestCaseSaveDTO();
        dto.setSeq(seq);
        dto.setInputData("1 2");
        dto.setOutputData("3");
        return dto;
    }

    private TestCase caseEntityOf(Long id, Long problemId) {
        TestCase testCase = new TestCase();
        testCase.setId(id);
        testCase.setProblemId(problemId);
        testCase.setSeq(1);
        return testCase;
    }
}
