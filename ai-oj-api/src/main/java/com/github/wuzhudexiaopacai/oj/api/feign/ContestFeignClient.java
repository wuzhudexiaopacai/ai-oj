package com.github.wuzhudexiaopacai.oj.api.feign;

import com.github.wuzhudexiaopacai.oj.api.dto.ContestRuntimeDTO;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 比赛服务内部契约
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@FeignClient(name = "ai-oj-contest", path = "/internal/contest", contextId = "contestFeignClient")
public interface ContestFeignClient {

    /**
     * 比赛运行时信息（时间窗口、题目集合、报名状态）
     *
     * @param contestId 比赛 ID
     * @param userId    用户 ID（可选，传入时回填是否报名）
     * @return 运行时信息
     */
    @GetMapping("/{contestId}/runtime")
    Result<ContestRuntimeDTO> runtime(@PathVariable("contestId") Long contestId,
                                      @RequestParam(value = "userId", required = false) Long userId);
}
