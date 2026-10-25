package com.github.wuzhudexiaopacai.oj.problem.service;

import com.github.wuzhudexiaopacai.oj.problem.dto.TestCaseSaveDTO;
import com.github.wuzhudexiaopacai.oj.problem.entity.TestCase;

import java.util.List;

/**
 * 评测用例管理服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface TestCaseService {

    /**
     * 用例列表（按 seq 升序）
     *
     * @param problemId 题目 ID
     * @return 用例列表
     */
    List<TestCase> listByProblem(Long problemId);

    /**
     * 新增加用例
     *
     * @param problemId 题目 ID
     * @param dto       用例入参
     * @return 用例 ID
     */
    Long create(Long problemId, TestCaseSaveDTO dto);

    /**
     * 更新用例
     *
     * @param problemId 题目 ID
     * @param caseId    用例 ID
     * @param dto       用例入参
     */
    void update(Long problemId, Long caseId, TestCaseSaveDTO dto);

    /**
     * 删除用例（逻辑删除）
     *
     * @param problemId 题目 ID
     * @param caseId    用例 ID
     */
    void delete(Long problemId, Long caseId);
}
