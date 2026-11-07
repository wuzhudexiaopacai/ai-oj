package com.github.wuzhudexiaopacai.oj.problem.service;

import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.problem.dto.SolutionSaveDTO;
import com.github.wuzhudexiaopacai.oj.problem.vo.SolutionLikeVO;
import com.github.wuzhudexiaopacai.oj.problem.vo.SolutionVO;

/**
 * 题解服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface SolutionService {

    /**
     * 题解分页（仅正常状态）
     *
     * @param problemId 题目 ID
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    PageResult<SolutionVO> page(Long problemId, long pageNum, long pageSize);

    /**
     * 题解详情
     *
     * @param id 题解 ID
     * @return 详情
     */
    SolutionVO detail(Long id);

    /**
     * 发表题解（需登录，接口层幂等防重）
     *
     * @param saveDTO 入参
     * @return 题解 ID
     */
    Long create(SolutionSaveDTO saveDTO);

    /**
     * 点赞/取消点赞（同一用户重复点击为切换，Redis 去重）
     *
     * @param id 题解 ID
     * @return 点赞结果
     */
    SolutionLikeVO toggleLike(Long id);
}
