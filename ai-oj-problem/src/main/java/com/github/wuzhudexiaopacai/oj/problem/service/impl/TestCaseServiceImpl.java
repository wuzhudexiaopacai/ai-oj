package com.github.wuzhudexiaopacai.oj.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.problem.dto.TestCaseSaveDTO;
import com.github.wuzhudexiaopacai.oj.problem.entity.Problem;
import com.github.wuzhudexiaopacai.oj.problem.entity.TestCase;
import com.github.wuzhudexiaopacai.oj.problem.mapper.ProblemMapper;
import com.github.wuzhudexiaopacai.oj.problem.mapper.TestCaseMapper;
import com.github.wuzhudexiaopacai.oj.problem.service.TestCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 评测用例管理服务实现
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class TestCaseServiceImpl implements TestCaseService {

    private final TestCaseMapper testCaseMapper;
    private final ProblemMapper problemMapper;

    @Override
    public List<TestCase> listByProblem(Long problemId) {
        return testCaseMapper.selectList(new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProblemId, problemId)
                .orderByAsc(TestCase::getSeq));
    }

    @Override
    public Long create(Long problemId, TestCaseSaveDTO dto) {
        checkProblem(problemId);
        validate(dto);
        TestCase testCase = BeanUtil.copyProperties(dto, TestCase.class, "id");
        testCase.setProblemId(problemId);
        testCaseMapper.insert(testCase);
        return testCase.getId();
    }

    @Override
    public void update(Long problemId, Long caseId, TestCaseSaveDTO dto) {
        checkProblem(problemId);
        validate(dto);
        TestCase exist = testCaseMapper.selectById(caseId);
        Asserts.notNull(exist, "用例不存在");
        Asserts.isTrue(exist.getProblemId().equals(problemId), "用例不属于该题目");
        TestCase testCase = BeanUtil.copyProperties(dto, TestCase.class, "id");
        testCase.setId(caseId);
        testCase.setProblemId(problemId);
        testCaseMapper.updateById(testCase);
    }

    @Override
    public void delete(Long problemId, Long caseId) {
        TestCase exist = testCaseMapper.selectById(caseId);
        Asserts.notNull(exist, "用例不存在");
        Asserts.isTrue(exist.getProblemId().equals(problemId), "用例不属于该题目");
        testCaseMapper.deleteById(caseId);
    }

    private void checkProblem(Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        Asserts.notNull(problem, "题目不存在");
    }

    /**
     * 校验：内容与文件二选一，输入输出至少各有一种载体
     */
    private void validate(TestCaseSaveDTO dto) {
        Asserts.isTrue(StrUtil.isNotBlank(dto.getInputData()) || StrUtil.isNotBlank(dto.getInputFile()),
                "用例输入不能为空（小数据填 inputData，大数据提交 inputFile）");
        Asserts.isTrue(StrUtil.isNotBlank(dto.getOutputData()) || StrUtil.isNotBlank(dto.getOutputFile()),
                "用例输出不能为空（小数据填 outputData，大数据提交 outputFile）");
    }
}
