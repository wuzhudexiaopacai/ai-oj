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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "比赛用户接口", description = "比赛用户端模块：比赛分页列表、我的比赛、比赛详情、报名与取消报名、排行榜查询；列表/详情/排行榜可匿名访问，我的比赛与报名相关接口需登录")
@RestController
@RequestMapping("/contest")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;

    private final ContestRegistrationService registrationService;

    private final ContestRankService rankService;

    @Operation(summary = "比赛分页（按状态/赛制过滤）", description = "分页查询比赛列表，支持按状态与赛制过滤，结果按开始时间倒序排列。可匿名访问，每条记录会回填已报名人数。")
    @GetMapping("/page")
    public Result<PageResult<ContestPageVO>> page(@ParameterObject ContestPageQueryDTO query) {
        return Result.ok(contestService.page(query));
    }

    @Operation(summary = "我报名的比赛", description = "查询当前登录用户已报名的比赛列表，按开始时间倒序排列并回填报名人数。需登录，未登录会被鉴权拦截；未报名任何比赛时返回空列表。")
    @GetMapping("/mine")
    public Result<List<ContestPageVO>> mine() {
        return Result.ok(contestService.mine());
    }

    @Operation(summary = "比赛详情（含题目列表）", description = "查询比赛详情，包含赛制、状态、时间、封榜时间、可见性、报名人数与当前用户报名状态。可匿名访问；比赛开始前题目列表一律不下发，私有赛（auth=2）未报名者同样不下发，管理员始终可见。")
    @GetMapping("/{id}")
    public Result<ContestDetailVO> detail(
            @Parameter(description = "比赛 ID", required = true, example = "1") @PathVariable("id") Long id) {
        return Result.ok(contestService.detail(id));
    }

    @Operation(summary = "报名比赛（私有赛需携带密码）", description = "当前登录用户报名指定比赛，仅 SCHEDULED（未开始）与 RUNNING（进行中）状态可报名。接口带幂等保护，重复报名会被拦截；私有赛必须携带正确密码。报名成功后会同步初始化判题侧比赛成绩记录，初始化失败则整体回滚。")
    @PostMapping("/{id}/register")
    @Idempotent(
            key = "'contest:register:' + #contestId + ':' + T(com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils).currentUserIdOrNull()",
            message = "请勿重复报名")
    public Result<Void> register(
            @Parameter(description = "比赛 ID", required = true, example = "1") @PathVariable("id") Long contestId,
            @Parameter(description = "私有赛密码；公开赛可省略，私有赛密码错误或为空时报名失败", required = false, example = "123456") @RequestParam(value = "password", required = false) String password) {
        registrationService.register(contestId, password);
        return Result.ok();
    }

    @Operation(summary = "取消报名", description = "取消当前登录用户对指定比赛的报名，报名记录物理删除，取消后可再次报名。需登录；未报名该比赛时返回业务异常。判题侧历史成绩记录保留，用于追溯与审计。")
    @DeleteMapping("/{id}/register")
    public Result<Void> cancelRegister(
            @Parameter(description = "比赛 ID", required = true, example = "1") @PathVariable("id") Long contestId) {
        registrationService.cancel(contestId);
        return Result.ok();
    }

    @Operation(summary = "比赛排行榜", description = "查询比赛排行榜，按赛制策略排序：ACM 按通过题数与罚时排名，OI 按总分排名；榜单仅包含比赛期间有提交记录的用户。到达封榜时间 sealTime 后普通用户只返回封榜快照，管理员仍可查看实时榜。可匿名访问。")
    @GetMapping("/{id}/rank")
    public Result<List<ContestRankVO>> rank(
            @Parameter(description = "比赛 ID", required = true, example = "1") @PathVariable("id") Long id) {
        return Result.ok(rankService.rank(id));
    }
}
