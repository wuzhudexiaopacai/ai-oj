package com.github.wuzhudexiaopacai.oj.contest.controller.internal;

import com.github.wuzhudexiaopacai.oj.api.dto.ContestRuntimeDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.ContestFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.contest.service.ContestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 比赛服务内部接口（实现 ContestFeignClient 契约）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "内部接口-比赛", description = "服务间 Feign 调用，仅供内部使用，不对外暴露")
@RestController
// @FeignClient 的 path 属性不会被 Spring MVC 识别，需显式声明类级路径与契约对齐
@RequestMapping("/internal/contest")
@RequiredArgsConstructor
public class InternalContestController implements ContestFeignClient {

    private final ContestService contestService;

    /**
     * 比赛运行时信息（判题服务提交校验 / 罚时基准使用）
     */
    @Override
    @Operation(summary = "比赛运行时信息（内部）", description = "供判题服务 ai-oj-judge 通过 ContestFeignClient 调用：提交时校验比赛状态、时间窗口与题目归属，并作为罚时基准；消费比赛开始/结束事件时读取题目集合。传入 userId 时回填该用户是否已报名，未传则跳过报名查询。比赛不存在时返回业务异常。")
    public Result<ContestRuntimeDTO> runtime(
            @Parameter(description = "比赛 ID", required = true, example = "1") Long contestId,
            @Parameter(description = "用户 ID，可选；传入时回填该用户在比赛中的报名状态", required = false, example = "10001") Long userId) {
        return Result.ok(contestService.runtime(contestId, userId));
    }
}
