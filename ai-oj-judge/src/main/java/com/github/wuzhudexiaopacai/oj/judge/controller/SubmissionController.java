package com.github.wuzhudexiaopacai.oj.judge.controller;

import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.judge.service.SubmissionService;
import com.github.wuzhudexiaopacai.oj.judge.vo.SubmissionDetailVO;
import com.github.wuzhudexiaopacai.oj.judge.vo.SubmissionPageVO;
import com.github.wuzhudexiaopacai.oj.judge.vo.UserProblemStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 提交记录接口
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "提交记录接口")
@RestController
@RequestMapping("/submission")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    /**
     * 当前用户提交分页（支持题目、状态、语言、时间范围、关键字筛选）
     */
    @Operation(summary = "当前用户提交分页")
    @GetMapping("/page")
    public Result<PageResult<SubmissionPageVO>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "problemId", required = false) Long problemId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.ok(submissionService.page(pageNum, pageSize, problemId, status,
                language, keyword, startTime, endTime));
    }

    /**
     * 当前用户的题目做题状态（题库列表标记「已通过 / 尝试过」）
     */
    @Operation(summary = "当前用户题目做题状态")
    @GetMapping("/user-problem-status")
    public Result<UserProblemStatusVO> userProblemStatus() {
        return Result.ok(submissionService.userProblemStatus());
    }

    /**
     * 按 submitId 查询提交详情（前端提交后轮询使用）
     */
    @Operation(summary = "按 submitId 查询提交详情")
    @GetMapping("/submit/{submitId}")
    public Result<SubmissionDetailVO> detailBySubmitId(@PathVariable("submitId") String submitId) {
        return Result.ok(submissionService.detailBySubmitId(submitId));
    }

    /**
     * 提交详情（含用例明细）
     */
    @Operation(summary = "提交详情")
    @GetMapping("/{id}")
    public Result<SubmissionDetailVO> detail(@PathVariable("id") Long id) {
        return Result.ok(submissionService.detail(id));
    }
}
