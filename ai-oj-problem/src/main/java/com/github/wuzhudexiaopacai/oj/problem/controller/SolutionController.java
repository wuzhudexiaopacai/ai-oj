package com.github.wuzhudexiaopacai.oj.problem.controller;

import com.github.wuzhudexiaopacai.oj.common.core.annotation.Idempotent;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.problem.dto.SolutionSaveDTO;
import com.github.wuzhudexiaopacai.oj.problem.service.SolutionService;
import com.github.wuzhudexiaopacai.oj.problem.vo.SolutionVO;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "题解接口")
@RestController
@RequestMapping("/solution")
@RequiredArgsConstructor
public class SolutionController {

    private final SolutionService solutionService;

    @Operation(summary = "题解分页")
    @GetMapping("/page")
    public Result<PageResult<SolutionVO>> page(@RequestParam("problemId") Long problemId,
                                               @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
                                               @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.ok(solutionService.page(problemId, pageNum, pageSize));
    }

    @Operation(summary = "题解详情")
    @GetMapping("/{id}")
    public Result<SolutionVO> detail(@PathVariable("id") Long id) {
        return Result.ok(solutionService.detail(id));
    }

    @Operation(summary = "发表题解")
    @PostMapping
    @Idempotent(key = "'solution:' + #saveDTO.problemId + ':' + "
            + "T(com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils).requireLogin().id",
            expire = 10, unit = TimeUnit.SECONDS, message = "请勿重复提交题解")
    public Result<Long> create(@Valid @RequestBody SolutionSaveDTO saveDTO) {
        return Result.ok(solutionService.create(saveDTO));
    }
}
