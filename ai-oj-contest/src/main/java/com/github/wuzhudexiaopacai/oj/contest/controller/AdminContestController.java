package com.github.wuzhudexiaopacai.oj.contest.controller;

import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.contest.dto.ContestProblemBindDTO;
import com.github.wuzhudexiaopacai.oj.contest.dto.ContestSaveDTO;
import com.github.wuzhudexiaopacai.oj.contest.service.ContestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 比赛管理端接口（权限码：contest:manage）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "比赛管理接口")
@RestController
@RequestMapping("/contest")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('contest:manage')")
public class AdminContestController {

    private final ContestService contestService;

    @Operation(summary = "新增比赛")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ContestSaveDTO saveDTO) {
        return Result.ok(contestService.create(saveDTO));
    }

    @Operation(summary = "更新比赛")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody ContestSaveDTO saveDTO) {
        contestService.update(id, saveDTO);
        return Result.ok();
    }

    @Operation(summary = "删除比赛")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        contestService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "绑定题目（display_id 按已有数量自动生成 A/B/C...）")
    @PostMapping("/{id}/problem")
    public Result<Long> bindProblem(@PathVariable("id") Long id,
                                    @Valid @RequestBody ContestProblemBindDTO bindDTO) {
        return Result.ok(contestService.bindProblem(id, bindDTO));
    }
}
