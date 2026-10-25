package com.github.wuzhudexiaopacai.oj.api.feign;

import com.github.wuzhudexiaopacai.oj.api.dto.ProblemBriefDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.ProblemJudgeDTO;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 题目服务内部契约
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@FeignClient(name = "ai-oj-problem", path = "/internal/problem", contextId = "problemFeignClient")
public interface ProblemFeignClient {

    /**
     * 获取判题所需的题目信息（含全部评测用例）
     *
     * @param problemId 题目 ID
     * @return 判题信息
     */
    @GetMapping("/{problemId}/judge-info")
    Result<ProblemJudgeDTO> getJudgeInfo(@PathVariable("problemId") Long problemId);

    /**
     * 获取题目简要信息（题号/标题/难度，不含用例）
     *
     * @param problemId 题目 ID
     * @return 简要信息，题目不存在时为 null
     */
    @GetMapping("/{problemId}/brief")
    Result<ProblemBriefDTO> getBrief(@PathVariable("problemId") Long problemId);

    /**
     * 按关键字检索题目 ID（题号 / 标题），用于提交记录按题目关键字筛选
     *
     * @param keyword 关键字
     * @return 匹配的题目 ID 列表，无匹配时为空列表
     */
    @GetMapping("/search-ids")
    Result<List<Long>> searchIds(@RequestParam("keyword") String keyword);
}
