package com.github.wuzhudexiaopacai.oj.problem.controller;

import com.github.wuzhudexiaopacai.oj.common.core.annotation.Idempotent;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.problem.dto.SolutionSaveDTO;
import com.github.wuzhudexiaopacai.oj.problem.service.SolutionService;
import com.github.wuzhudexiaopacai.oj.problem.vo.SolutionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 题解接口
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "题解接口", description = "题目题解的浏览与发表（用户端）：分页、详情、发表；需登录（网关统一鉴权），发表接口额外做 10 秒幂等防重")
@RestController
@RequestMapping("/solution")
@RequiredArgsConstructor
public class SolutionController {

    private final SolutionService solutionService;

    @Operation(summary = "题解分页", description = "分页查询指定题目下状态正常的题解（status=1），按点赞数、发布时间倒序。列表不返回 content 正文以减小报文（content 为 null）。pageNum 从 1 开始，pageSize 默认 10。")
    @GetMapping("/page")
    public Result<PageResult<SolutionVO>> page(
            @RequestParam("problemId")
            @Parameter(description = "题目 ID，不能为空", required = true, example = "10001") Long problemId,
            @RequestParam(value = "pageNum", defaultValue = "1")
            @Parameter(description = "页码，从 1 开始，默认 1", required = false, example = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10")
            @Parameter(description = "每页条数，默认 10", required = false, example = "10") long pageSize) {
        return Result.ok(solutionService.page(problemId, pageNum, pageSize));
    }

    @Operation(summary = "题解详情", description = "按题解 ID 查询详情，返回完整 Markdown 正文。仅状态正常的题解可见，待审核题解返回业务失败提示「题解待审核中」，题解不存在时报「题解不存在」。")
    @GetMapping("/{id}")
    public Result<SolutionVO> detail(
            @PathVariable("id")
            @Parameter(description = "题解 ID", required = true, example = "20001") Long id) {
        return Result.ok(solutionService.detail(id));
    }

    @Operation(summary = "发表题解", description = "当前登录用户发表题解，题目必须存在；作者、初始点赞数（0）与状态（正常）由服务端写入。同一用户对同一题目 10 秒内重复提交会被幂等拦截并提示「请勿重复提交题解」。返回新建题解 ID。")
    @PostMapping
    @Idempotent(key = "'solution:' + #saveDTO.problemId + ':' + "
            + "T(com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils).requireLogin().id",
            expire = 10, unit = TimeUnit.SECONDS, message = "请勿重复提交题解")
    public Result<Long> create(@Valid @RequestBody SolutionSaveDTO saveDTO) {
        return Result.ok(solutionService.create(saveDTO));
    }
}
