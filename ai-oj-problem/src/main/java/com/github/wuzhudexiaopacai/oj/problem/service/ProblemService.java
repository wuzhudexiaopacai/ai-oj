package com.github.wuzhudexiaopacai.oj.problem.service;

import com.github.wuzhudexiaopacai.oj.api.dto.ProblemBriefDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.ProblemJudgeDTO;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.problem.dto.ProblemPageQueryDTO;
import com.github.wuzhudexiaopacai.oj.problem.dto.ProblemSaveDTO;
import com.github.wuzhudexiaopacai.oj.problem.entity.Problem;
import com.github.wuzhudexiaopacai.oj.problem.vo.ProblemDetailVO;
import com.github.wuzhudexiaopacai.oj.problem.vo.ProblemImportResultVO;
import com.github.wuzhudexiaopacai.oj.problem.vo.ProblemPageVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 题目服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface ProblemService {

    /**
     * 用户侧分页查询（仅上架且公开，带列表缓存）
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<ProblemPageVO> page(ProblemPageQueryDTO query);

    /**
     * 管理端分页查询（含下架/私有题目，不走缓存）
     *
     * @param query 查询条件
     * @return 分页结果（完整实体）
     */
    PageResult<Problem> adminPage(ProblemPageQueryDTO query);

    /**
     * 管理端题目详情（完整实体 + 标签 + 用例 + 语言模板，供编辑回显）
     *
     * @param id 题目 ID
     * @return 编辑用完整数据
     */
    ProblemSaveDTO adminDetail(Long id);

    /**
     * 题目详情（防穿透/防击穿/随机 TTL）
     *
     * @param problemKey 展示题号
     * @return 详情
     */
    ProblemDetailVO detail(String problemKey);

    /**
     * ES 全文检索（ES 不可用时降级 DB LIKE）
     *
     * @param keyword 关键字
     * @return 命中题目列表
     */
    List<ProblemPageVO> search(String keyword);

    /**
     * 判题信息（内部 Feign 契约）
     *
     * @param problemId 题目 ID
     * @return 判题信息
     */
    ProblemJudgeDTO judgeInfo(Long problemId);

    /**
     * 题目简要信息（内部 Feign 契约，不含评测用例）
     *
     * @param problemId 题目 ID
     * @return 简要信息，题目不存在时为 null
     */
    ProblemBriefDTO brief(Long problemId);

    /**
     * 新增题目（题目+用例+语言模板+标签一体保存）
     *
     * @param saveDTO 保存入参
     * @return 题目 ID
     */
    Long create(ProblemSaveDTO saveDTO);

    /**
     * 更新题目
     *
     * @param id      题目 ID
     * @param saveDTO 保存入参
     */
    void update(Long id, ProblemSaveDTO saveDTO);

    /**
     * 删除题目（逻辑删除）
     *
     * @param id 题目 ID
     */
    void delete(Long id);

    /**
     * 导出题目 Excel
     *
     * @param response HTTP 响应
     */
    void export(HttpServletResponse response);

    /**
     * 导入题目 Excel（逐行校验，错误行收集返回）
     *
     * @param file Excel 文件
     * @return 导入结果
     */
    ProblemImportResultVO importProblems(MultipartFile file);
}
