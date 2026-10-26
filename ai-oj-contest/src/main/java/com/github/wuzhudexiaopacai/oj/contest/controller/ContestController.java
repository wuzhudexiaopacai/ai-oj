package com.github.wuzhudexiaopacai.oj.contest.controller;

import com.github.wuzhudexiaopacai.oj.common.core.annotation.Idempotent;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.contest.dto.ContestPageQueryDTO;
import com.github.wuzhudexiaopacai.oj.contest.service.ContestRankService;
import com.github.wuzhudexiaopacai.oj.contest.service.ContestRegistrationService;
import com.github.wuzhudexiaopacai.oj.contest.service.ContestService;
import com.github.wuzhudexiaopacai.oj.contest.vo.ContestDetailVO;
import com.github.wuzhudexiaopacai.oj.contest.vo.ContestPageVO;
import com.github.wuzhudexiaopacai.oj.contest.vo.ContestRankVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 比赛用户端接口（分页查询、详情、报名/取消、排行榜）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "比赛用户接口")
@RestController
@RequestMapping("/contest")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;

    private final ContestRegistrationService registrationService;

    private final ContestRankService rankService;

    @Operation(summary = "比赛分页（按状态/赛制过滤）")
    @GetMapping("/page")
    public Result<PageResult<ContestPageVO>> page(ContestPageQueryDTO query) {
        return Result.ok(contestService.page(query));
    }

    @Operation(summary = "我报名的比赛")
    @GetMapping("/mine")
    public Result<List<ContestPageVO>> mine() {
        return Result.ok(contestService.mine());
    }

    @Operation(summary = "比赛详情（含题目列表）")
    @GetMapping("/{id}")
    public Result<ContestDetailVO> detail(@PathVariable("id") Long id) {
        return Result.ok(contestService.detail(id));
    }

    @Operation(summary = "报名比赛（私有赛需携带密码）")
    @PostMapping("/{id}/register")
    @Idempotent(
            key = "'contest:register:' + #contestId + ':' + T(com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils).currentUserIdOrNull()",
            message = "请勿重复报名")
    public Result<Void> register(@PathVariable("id") Long contestId,
                                 @RequestParam(value = "password", required = false) String password) {
        registrationService.register(contestId, password);
        return Result.ok();
    }

    @Operation(summary = "取消报名")
    @DeleteMapping("/{id}/register")
    public Result<Void> cancelRegister(@PathVariable("id") Long contestId) {
        registrationService.cancel(contestId);
        return Result.ok();
    }

    @Operation(summary = "比赛排行榜")
    @GetMapping("/{id}/rank")
    public Result<List<ContestRankVO>> rank(@PathVariable("id") Long id) {
        return Result.ok(rankService.rank(id));
    }
}
