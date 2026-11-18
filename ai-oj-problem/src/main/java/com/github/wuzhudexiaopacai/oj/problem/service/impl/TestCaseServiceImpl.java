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

    /**
     * 状态：下架（草稿）
     */
    private static final int STATUS_OFFLINE = 0;

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
        checkSeqUnique(problemId, dto.getSeq(), null);
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
        checkSeqUnique(problemId, dto.getSeq(), caseId);
        TestCase testCase = BeanUtil.copyProperties(dto, TestCase.class, "id");
        testCase.setId(caseId);
        testCase.setProblemId(problemId);
        testCaseMapper.updateById(testCase);
    }

    @Override
    public void delete(Long problemId, Long caseId) {
        Problem problem = checkProblem(problemId);
        TestCase exist = testCaseMapper.selectById(caseId);
        Asserts.notNull(exist, "用例不存在");
        Asserts.isTrue(exist.getProblemId().equals(problemId), "用例不属于该题目");
        // 上架/比赛中题目不允许删掉最后一条用例，否则判题会因无用例直接判 AC
        if (problem.getStatus() != null && problem.getStatus() != STATUS_OFFLINE) {
            Long remaining = testCaseMapper.selectCount(new LambdaQueryWrapper<TestCase>()
                    .eq(TestCase::getProblemId, problemId)
                    .ne(TestCase::getId, caseId));
            Asserts.isTrue(remaining != null && remaining > 0, "上架题目至少需要保留一个评测用例");
        }
        testCaseMapper.deleteById(caseId);
    }

    private Problem checkProblem(Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        Asserts.notNull(problem, "题目不存在");
        return problem;
    }

    /**
     * 校验：内容与文件二选一，输入输出至少各有一种载体；seq 为正整数
     */
    private void validate(TestCaseSaveDTO dto) {
        Asserts.notNull(dto.getSeq(), "用例序号不能为空");
        Asserts.isTrue(dto.getSeq() > 0, "用例序号必须为正整数");
        Asserts.isTrue(StrUtil.isNotBlank(dto.getInputData()) || StrUtil.isNotBlank(dto.getInputFile()),
                "用例输入不能为空（小数据填 inputData，大数据提交 inputFile）");
        Asserts.isTrue(StrUtil.isNotBlank(dto.getOutputData()) || StrUtil.isNotBlank(dto.getOutputFile()),
                "用例输出不能为空（小数据填 outputData，大数据提交 outputFile）");
    }

    /**
     * 用例序号题目内唯一（排除自身），避免沙箱用例文件互相覆盖
     *
     * @param problemId 题目 ID
     * @param seq       用例序号
     * @param excludeId 需要排除的用例 ID（更新时传当前用例）
     */
    private void checkSeqUnique(Long problemId, Integer seq, Long excludeId) {
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProblemId, problemId)
                .eq(TestCase::getSeq, seq);
        if (excludeId != null) {
            wrapper.ne(TestCase::getId, excludeId);
        }
        Long count = testCaseMapper.selectCount(wrapper);
        Asserts.isTrue(count == null || count == 0, "用例序号已存在：" + seq);
    }
}
