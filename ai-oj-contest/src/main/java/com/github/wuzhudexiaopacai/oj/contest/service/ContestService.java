package com.github.wuzhudexiaopacai.oj.contest.service;

import com.github.wuzhudexiaopacai.oj.api.dto.ContestRuntimeDTO;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.contest.dto.ContestPageQueryDTO;
import com.github.wuzhudexiaopacai.oj.contest.dto.ContestProblemBindDTO;
import com.github.wuzhudexiaopacai.oj.contest.dto.ContestSaveDTO;
import com.github.wuzhudexiaopacai.oj.contest.vo.ContestDetailVO;
import com.github.wuzhudexiaopacai.oj.contest.vo.ContestPageVO;

/**
 * 比赛服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface ContestService {

    /**
     * 分页查询比赛列表（支持按状态、赛制过滤）
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<ContestPageVO> page(ContestPageQueryDTO query);

    /**
     * 查询比赛详情（含题目列表；带 Redis 缓存 contest:info:{id} TTL 10min）
     *
     * @param id 比赛 ID
     * @return 详情
     */
    ContestDetailVO detail(Long id);

    /**
     * 创建比赛（默认状态 SCHEDULED，创建者为当前登录用户）
     *
     * @param saveDTO 入参
     * @return 比赛 ID
     */
    Long create(ContestSaveDTO saveDTO);

    /**
     * 更新比赛（乐观锁并发控制；写操作后主动失效缓存）
     *
     * @param id      比赛 ID
     * @param saveDTO 入参
     */
    void update(Long id, ContestSaveDTO saveDTO);

    /**
     * 删除比赛（逻辑删除；同时级联逻辑删除已绑定题目）
     *
     * @param id 比赛 ID
     */
    void delete(Long id);

    /**
     * 绑定题目到比赛（display_id 按已有数量自动生成 A/B/C...）
     *
     * @param contestId 比赛 ID
     * @param bindDTO   绑定入参
     * @return 比赛题目关联 ID
     */
    Long bindProblem(Long contestId, ContestProblemBindDTO bindDTO);

    /**
     * 比赛运行时信息（判题服务校验比赛提交用）
     *
     * @param contestId 比赛 ID
     * @param userId    用户 ID（可选，传入时回填是否报名）
     * @return 运行时信息
     */
    ContestRuntimeDTO runtime(Long contestId, Long userId);
}
