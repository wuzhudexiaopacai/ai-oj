package com.github.wuzhudexiaopacai.oj.judge.controller;

import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.judge.dto.JudgeRunRequest;
import com.github.wuzhudexiaopacai.oj.judge.dto.JudgeSubmitRequest;
import com.github.wuzhudexiaopacai.oj.judge.service.JudgeService;
import com.github.wuzhudexiaopacai.oj.judge.service.SubmissionService;
import com.github.wuzhudexiaopacai.oj.judge.vo.JudgeProgressVO;
import com.github.wuzhudexiaopacai.oj.judge.vo.JudgeRunVO;
import com.github.wuzhudexiaopacai.oj.judge.vo.SubmitResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 判题接口
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "判题接口")
@RestController
@RequestMapping("/judge")
@RequiredArgsConstructor
public class JudgeController {

    private final JudgeService judgeService;

    private final SubmissionService submissionService;

    /**
     * 提交判题
     */
    @Operation(summary = "提交判题")
    @PostMapping("/submit")
    public Result<SubmitResultVO> submit(@Valid @RequestBody JudgeSubmitRequest req) {
        return Result.ok(judgeService.submit(req));
    }

    /**
     * 自测运行（不入库）
     */
    @Operation(summary = "自测运行")
    @PostMapping("/run")
    public Result<JudgeRunVO> run(@Valid @RequestBody JudgeRunRequest req) {
        return Result.ok(judgeService.run(req));
    }

    /**
     * 判题进度（WebSocket 断线轮询兜底）
     */
    @Operation(summary = "判题进度")
    @GetMapping("/progress")
    public Result<JudgeProgressVO> progress(@RequestParam("submitId") String submitId) {
        return Result.ok(submissionService.progress(submitId));
    }
}
