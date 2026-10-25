package com.github.wuzhudexiaopacai.oj.judge.controller.internal;

import com.github.wuzhudexiaopacai.oj.api.dto.ContestProblemStatusDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.ContestRecordDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.JudgeFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.judge.service.ContestRecordService;
import com.github.wuzhudexiaopacai.oj.judge.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 判题服务内部接口（实现 JudgeFeignClient 契约）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@RestController
// @FeignClient 的 path 属性不会被 Spring MVC 识别，需显式声明类级路径与契约对齐
@RequestMapping("/internal/judge")
@RequiredArgsConstructor
public class InternalJudgeController implements JudgeFeignClient {

    private final ContestRecordService contestRecordService;

    private final SubmissionService submissionService;

    /**
     * 初始化比赛成绩记录（Seata 分布式事务参与者）
     */
    @Override
    public Result<Void> initContestRecord(Long contestId, Long userId, String username) {
        contestRecordService.initRecord(contestId, userId, username);
        return Result.ok();
    }

    /**
     * 查询比赛成绩列表
     */
    @Override
    public Result<List<ContestRecordDTO>> listContestRecord(Long contestId) {
        return Result.ok(contestRecordService.listByContest(contestId));
    }

    /**
     * 查询用户在指定比赛内的题目通过状态（比赛题目列表标记「已通过 / 尝试过」）
     */
    @Override
    public Result<ContestProblemStatusDTO> contestProblemStatus(Long contestId, Long userId) {
        return Result.ok(submissionService.contestProblemStatus(contestId, userId));
    }
}
