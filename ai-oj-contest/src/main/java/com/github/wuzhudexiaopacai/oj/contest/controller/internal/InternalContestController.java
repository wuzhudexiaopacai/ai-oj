package com.github.wuzhudexiaopacai.oj.contest.controller.internal;

import com.github.wuzhudexiaopacai.oj.api.dto.ContestRuntimeDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.ContestFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.contest.service.ContestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 比赛服务内部接口（实现 ContestFeignClient 契约）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
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
    public Result<ContestRuntimeDTO> runtime(Long contestId, Long userId) {
        return Result.ok(contestService.runtime(contestId, userId));
    }
}
