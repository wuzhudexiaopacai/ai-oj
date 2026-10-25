package com.github.wuzhudexiaopacai.oj.ai.controller;

import com.github.wuzhudexiaopacai.oj.ai.dto.ReviewRequestDTO;
import com.github.wuzhudexiaopacai.oj.ai.dto.TaskSubmitDTO;
import com.github.wuzhudexiaopacai.oj.ai.service.AiChatService;
import com.github.wuzhudexiaopacai.oj.ai.service.AiTaskService;
import com.github.wuzhudexiaopacai.oj.ai.vo.TaskStatusVO;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话接口（流式对话 / 代码评审 / 异步任务）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "AI 对话接口")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    private final AiTaskService aiTaskService;

    @Operation(summary = "流式对话（SSE）")
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("sessionId") Long sessionId,
                             @RequestParam("question") String question) {
        return aiChatService.streamChat(sessionId, question);
    }

    @Operation(summary = "同步代码评审")
    @PostMapping("/review")
    public Result<String> review(@Validated @RequestBody ReviewRequestDTO req) {
        return Result.ok(aiChatService.review(req));
    }

    @Operation(summary = "提交异步任务（出题/批量评审）")
    @PostMapping("/task/submit")
    public Result<String> submitTask(@Validated @RequestBody TaskSubmitDTO req) {
        return Result.ok(aiTaskService.submitTask(req));
    }

    @Operation(summary = "查询异步任务状态与结果")
    @GetMapping("/task/{taskId}")
    public Result<TaskStatusVO> task(@PathVariable("taskId") String taskId) {
        return Result.ok(aiTaskService.getTask(taskId));
    }
}
