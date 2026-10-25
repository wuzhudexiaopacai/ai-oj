package com.github.wuzhudexiaopacai.oj.api.feign;

import com.github.wuzhudexiaopacai.oj.api.dto.UserBriefDTO;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 用户服务内部契约
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@FeignClient(name = "ai-oj-auth", path = "/internal/user", contextId = "authFeignClient")
public interface AuthFeignClient {

    /**
     * 批量获取用户简要信息（排行榜等聚合场景）
     *
     * @param userIds 用户 ID 列表
     * @return 用户简要信息
     */
    @PostMapping("/batch-brief")
    Result<List<UserBriefDTO>> batchGetUserBrief(@RequestBody List<Long> userIds);
}
