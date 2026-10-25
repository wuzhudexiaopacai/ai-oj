package com.github.wuzhudexiaopacai.oj.problem.controller;

import com.github.wuzhudexiaopacai.oj.api.dto.ProblemBriefDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.ProblemJudgeDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.ProblemFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.problem.service.ProblemService;
import com.github.wuzhudexiaopacai.oj.problem.vo.ProblemPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 题目内部接口（实现 ProblemFeignClient 契约，仅供内网/Feign 调用）
 * <p>
 * 注意：@FeignClient 的 path 属性不会被 Spring MVC 识别，类级路径需显式声明
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@RestController
@RequestMapping("/internal/problem")
@RequiredArgsConstructor
public class InternalProblemController implements ProblemFeignClient {

    private final ProblemService problemService;

    @Override
    public Result<ProblemJudgeDTO> getJudgeInfo(Long problemId) {
        return Result.ok(problemService.judgeInfo(problemId));
    }

    @Override
    public Result<ProblemBriefDTO> getBrief(Long problemId) {
        return Result.ok(problemService.brief(problemId));
    }

    @Override
    public Result<List<Long>> searchIds(String keyword) {
        return Result.ok(problemService.search(keyword).stream()
                .map(ProblemPageVO::getId)
                // 限制命中数量，避免调用方拼接过长的 IN 条件
                .limit(200)
                .toList());
    }
}
