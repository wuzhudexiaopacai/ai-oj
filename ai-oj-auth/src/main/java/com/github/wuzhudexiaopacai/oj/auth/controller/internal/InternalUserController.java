package com.github.wuzhudexiaopacai.oj.auth.controller.internal;

import com.github.wuzhudexiaopacai.oj.api.dto.UserBriefDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.AuthFeignClient;
import com.github.wuzhudexiaopacai.oj.auth.service.UserService;
import com.github.wuzhudexiaopacai.oj.common.core.constant.CommonConstants;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.core.result.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 用户服务内部接口（仅供服务间 Feign 调用，外部请求会被网关剥离内部标识头）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "内部接口-用户", description = "服务间 Feign 调用，仅供内部使用，不对外暴露")
@RestController
@RequestMapping("/internal/user")
@RequiredArgsConstructor
public class InternalUserController implements AuthFeignClient {

    private final UserService userService;

    /**
     * 批量获取用户简要信息（排行榜等聚合场景）
     *
     * @param userIds 用户 ID 列表
     * @return 用户简要信息
     */
    @Operation(summary = "批量获取用户简要信息",
            description = "供 contest 服务（比赛排行榜聚合）通过 Feign 调用，按用户 ID 批量返回 ID、登录名、昵称、头像与简介。空列表直接返回空集合，重复 ID 自动去重；调用方必须携带网关注入的内部调用标识头，否则返回 403。")
    @Override
    @PostMapping("/batch-brief")
    public Result<List<UserBriefDTO>> batchGetUserBrief(@RequestBody List<Long> userIds) {
        checkInternalCall();
        return Result.ok(userService.batchGetUserBrief(userIds));
    }

    /**
     * 校验内部调用标识（防止绕过网关直连伪造）
     */
    private void checkInternalCall() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();
        String internalCall = request == null ? null : request.getHeader(CommonConstants.HEADER_INTERNAL_CALL);
        if (!"true".equals(internalCall)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }
}
