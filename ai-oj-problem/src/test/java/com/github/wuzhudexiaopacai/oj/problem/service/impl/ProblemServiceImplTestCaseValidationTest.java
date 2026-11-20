package com.github.wuzhudexiaopacai.oj.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.problem.dto.TestCaseSaveDTO;
import com.github.wuzhudexiaopacai.oj.problem.mapper.TestCaseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 题目保存时的评测用例校验测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ProblemServiceImplTestCaseValidationTest {

    @Mock
    private TestCaseMapper testCaseMapper;

    @InjectMocks
    private ProblemServiceImpl problemService;

    @Test
    void emptyCasesShouldBeRejectedWhenOnline() {
        BizException ex = assertThrows(BizException.class,
                () -> problemService.validateTestCases(null, List.of(), 1));
        assertTrue(ex.getMessage().contains("至少需要配置一个评测用例"));
    }

    @Test
    void emptyCasesShouldBeAllowedForDraft() {
        assertDoesNotThrow(() -> problemService.validateTestCases(null, List.of(), 0));
    }

    @Test
    void duplicatedSeqShouldBeRejected() {
        BizException ex = assertThrows(BizException.class,
                () -> problemService.validateTestCases(null, List.of(caseOf(1), caseOf(1)), 1));
        assertTrue(ex.getMessage().contains("用例序号重复"));
    }

    @Test
    void nullCasesShouldBeRejectedOnCreate() {
        BizException ex = assertThrows(BizException.class,
                () -> problemService.validateTestCases(null, null, 1));
        assertTrue(ex.getMessage().contains("至少需要配置一个评测用例"));
    }

    @Test
    void nullCasesShouldPassWhenDbHasCases() {
        when(testCaseMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        assertDoesNotThrow(() -> problemService.validateTestCases(1L, null, 1));
    }

    @Test
    void nullCasesShouldBeRejectedWhenDbHasNoCase() {
        when(testCaseMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        assertThrows(BizException.class, () -> problemService.validateTestCases(1L, null, 1));
    }

    @Test
    void missingInputCarrierShouldBeRejected() {
        TestCaseSaveDTO dto = new TestCaseSaveDTO();
        dto.setSeq(1);
        dto.setOutputData("3");

        BizException ex = assertThrows(BizException.class,
                () -> problemService.validateTestCases(null, List.of(dto), 1));
        assertTrue(ex.getMessage().contains("用例输入不能为空"));
    }

    @Test
    void missingOutputCarrierShouldBeRejected() {
        TestCaseSaveDTO dto = new TestCaseSaveDTO();
        dto.setSeq(1);
        dto.setInputData("1 2");

        BizException ex = assertThrows(BizException.class,
                () -> problemService.validateTestCases(null, List.of(dto), 1));
        assertTrue(ex.getMessage().contains("用例输出不能为空"));
    }

    @Test
    void fileCarrierShouldBeAccepted() {
        TestCaseSaveDTO dto = new TestCaseSaveDTO();
        dto.setSeq(1);
        dto.setInputFile("files/md5/1.in");
        dto.setOutputFile("files/md5/1.out");

        assertDoesNotThrow(() -> problemService.validateTestCases(null, List.of(dto), 1));
    }

    @Test
    void spjWithoutLanguageShouldBeRejected() {
        BizException ex = assertThrows(BizException.class,
                () -> problemService.validateSpj(1, null, "int main() {}"));
        assertTrue(ex.getMessage().contains("必须指定 SPJ 语言"));
    }

    @Test
    void spjWithoutCodeShouldBeRejected() {
        BizException ex = assertThrows(BizException.class,
                () -> problemService.validateSpj(1, "cpp", " "));
        assertTrue(ex.getMessage().contains("必须提供 SPJ 代码"));
    }

    @Test
    void spjDisabledShouldPass() {
        assertDoesNotThrow(() -> problemService.validateSpj(0, null, null));
    }

    private TestCaseSaveDTO caseOf(int seq) {
        TestCaseSaveDTO dto = new TestCaseSaveDTO();
        dto.setSeq(seq);
        dto.setInputData("1 2");
        dto.setOutputData("3");
        return dto;
    }
}
