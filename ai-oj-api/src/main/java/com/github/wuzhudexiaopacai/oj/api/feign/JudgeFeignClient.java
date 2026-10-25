package com.github.wuzhudexiaopacai.oj.api.feign;

import com.github.wuzhudexiaopacai.oj.api.dto.ContestRecordDTO;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 判题服务内部契约
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@FeignClient(name = "ai-oj-judge", path = "/internal/judge", contextId = "judgeFeignClient")
public interface JudgeFeignClient {

    /**
     * 初始化比赛成绩记录（Seata 分布式事务参与者：与报名操作同生共死）
     *
     * @param contestId 比赛 ID
     * @param userId    用户 ID
     * @param username  用户名快照
     * @return 结果
     */
    @PostMapping("/contest-record/init")
    Result<Void> initContestRecord(@RequestParam("contestId") Long contestId,
                                   @RequestParam("userId") Long userId,
                                   @RequestParam("username") String username);

    /**
     * 查询比赛成绩列表（排行榜数据源，比赛服务本地缓存重建时调用）
     *
     * @param contestId 比赛 ID
     * @return 成绩列表（未排序，由调用方按赛制策略排序）
     */
    @GetMapping("/contest-record/list")
    Result<List<ContestRecordDTO>> listContestRecord(@RequestParam("contestId") Long contestId);
}
