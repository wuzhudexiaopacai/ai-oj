package com.github.wuzhudexiaopacai.oj.judge.controller.internal;

import com.github.wuzhudexiaopacai.oj.api.dto.ContestProblemStatusDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.ContestRecordDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.JudgeFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.judge.service.ContestRecordService;
import com.github.wuzhudexiaopacai.oj.judge.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "内部接口-判题", description = "服务间 Feign 调用，仅供内部使用，不对外暴露；调用方为比赛服务（ai-oj-contest），用于报名初始化成绩记录、排行榜数据源与比赛题目通过状态查询")
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
    @Operation(summary = "初始化比赛成绩记录", description = "由比赛服务（ai-oj-contest）报名流程通过 Feign 调用，作为 Seata 分布式事务参与者：为指定用户在比赛中初始化成绩记录（总分/通过数/罚时置 0），唯一键 uk_contest_user 保证重复调用幂等。远程调用失败会使比赛服务本地报名事务一并回滚；仅供内部使用。")
    @Override
    public Result<Void> initContestRecord(
            @Parameter(description = "比赛 ID", required = true, example = "1950000000000000001") Long contestId,
            @Parameter(description = "用户 ID", required = true, example = "1950000000000000002") Long userId,
            @Parameter(description = "用户名快照，为空时按空字符串落库", required = true, example = "alice") String username) {
        contestRecordService.initRecord(contestId, userId, username);
        return Result.ok();
    }

    /**
     * 查询比赛成绩列表
     */
    @Operation(summary = "查询比赛成绩列表", description = "由比赛服务（ai-oj-contest）构建排行榜时通过 Feign 调用，作为排行榜数据源。只返回该比赛内有提交记录的用户，未参赛的报名者不会出现在结果中；结果未排序，由调用方按赛制策略排序；仅供内部使用。")
    @Override
    public Result<List<ContestRecordDTO>> listContestRecord(
            @Parameter(description = "比赛 ID", required = true, example = "1950000000000000001") Long contestId) {
        return Result.ok(contestRecordService.listByContest(contestId));
    }

    /**
     * 查询用户在指定比赛内的题目通过状态（比赛题目列表标记「已通过 / 尝试过」）
     */
    @Operation(summary = "查询比赛内题目通过状态", description = "由比赛服务（ai-oj-contest）在比赛详情页标记题目「已通过 / 尝试过」时通过 Feign 调用。仅统计携带该 contestId 的提交，赛前历史提交不计入；返回已通过（状态 3 答案正确）与提交过的题目 ID；仅供内部使用。")
    @Override
    public Result<ContestProblemStatusDTO> contestProblemStatus(
            @Parameter(description = "比赛 ID", required = true, example = "1950000000000000001") Long contestId,
            @Parameter(description = "用户 ID", required = true, example = "1950000000000000002") Long userId) {
        return Result.ok(submissionService.contestProblemStatus(contestId, userId));
    }
}
