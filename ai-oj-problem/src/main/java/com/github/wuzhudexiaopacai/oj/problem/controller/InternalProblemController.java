package com.github.wuzhudexiaopacai.oj.problem.controller;

import com.github.wuzhudexiaopacai.oj.api.dto.ProblemBriefDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.ProblemJudgeDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.ProblemFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.problem.service.ProblemService;
import com.github.wuzhudexiaopacai.oj.problem.vo.ProblemPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "内部接口-题目", description = "服务间 Feign 调用，仅供内部使用，不对外暴露")
@RestController
@RequestMapping("/internal/problem")
@RequiredArgsConstructor
public class InternalProblemController implements ProblemFeignClient {

    private final ProblemService problemService;

    @Operation(summary = "获取判题信息（含全部用例）", description = "供判题服务（ai-oj-judge）评测前调用：返回题目限制（时间/内存）、SPJ 配置与全部评测用例，用例按 seq 升序。小数据用例内联返回 input/output，大数据用例返回 MinIO objectKey 由判题服务自行拉取；题目不存在时返回业务失败提示「题目不存在」。")
    @Override
    public Result<ProblemJudgeDTO> getJudgeInfo(
            @Parameter(description = "题目 ID", required = true, example = "10001") Long problemId) {
        return Result.ok(problemService.judgeInfo(problemId));
    }

    @Operation(summary = "获取题目简要信息", description = "供 submission（提交记录）等服务展示题目：返回题目 ID、题号、标题与难度，不含描述与评测用例。题目不存在时 data 为 null，不抛出异常。")
    @Override
    public Result<ProblemBriefDTO> getBrief(
            @Parameter(description = "题目 ID", required = true, example = "10001") Long problemId) {
        return Result.ok(problemService.brief(problemId));
    }

    @Operation(summary = "按关键字检索题目 ID", description = "供 submission 服务按题目关键字筛选提交记录：复用题目全文检索（ES，失败或无命中时降级数据库 LIKE），仅取已上架且公开的题目，最多返回 200 个 ID 以避免调用方拼接过长的 IN 条件。无命中时返回空列表。")
    @Override
    public Result<List<Long>> searchIds(
            @Parameter(description = "搜索关键字，匹配题号/标题/描述，不能为空",
                    required = true, example = "动态规划") String keyword) {
        return Result.ok(problemService.search(keyword).stream()
                .map(ProblemPageVO::getId)
                // 限制命中数量，避免调用方拼接过长的 IN 条件
                .limit(200)
                .toList());
    }
}
