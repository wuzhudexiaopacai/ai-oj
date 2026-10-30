package com.github.wuzhudexiaopacai.oj.ai.controller;

import com.github.wuzhudexiaopacai.oj.ai.dto.ChatStreamRequest;
import com.github.wuzhudexiaopacai.oj.ai.dto.ProblemGenerateDTO;
import com.github.wuzhudexiaopacai.oj.ai.dto.ReviewRequestDTO;
import com.github.wuzhudexiaopacai.oj.ai.dto.TaskSubmitDTO;
import com.github.wuzhudexiaopacai.oj.ai.service.AiChatService;
import com.github.wuzhudexiaopacai.oj.ai.service.AiGenerateService;
import com.github.wuzhudexiaopacai.oj.ai.service.AiTaskService;
import com.github.wuzhudexiaopacai.oj.ai.vo.ProblemDraftVO;
import com.github.wuzhudexiaopacai.oj.ai.vo.TaskStatusVO;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话接口（流式对话 / 代码评审 / 异步任务）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "AI 对话接口", description = "流式对话（SSE）、同步代码评审与异步任务提交/查询；经网关 JWT 鉴权，所有接口需登录")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    private final AiTaskService aiTaskService;

    private final AiGenerateService aiGenerateService;

    @Operation(summary = "AI 出题（同步生成结构化题目草稿）",
            description = "根据知识点 topic、难度 difficulty（0/1/2）、可选自定义要求 requirement 与用例数 caseCount（默认 10），"
                    + "同步调用大模型生成一道含题面、输入输出说明与多组评测用例的结构化题目草稿，返回 JSON 供前端一键回填编辑表单；需登录。")
    @PostMapping("/generate")
    public Result<ProblemDraftVO> generate(@Validated @RequestBody ProblemGenerateDTO req) {
        return Result.ok(aiGenerateService.generate(req));
    }

    @Operation(summary = "流式对话（SSE）",
            description = "以 text/event-stream 返回，每个 SSE 事件的 data 为一个模型输出片段（Markdown 文本）。服务端按会话场景（chat/hint/review/generate）渲染提示词，注入题目上下文与本次上报的代码；每用户默认限流 10 次/分钟，超时 180 秒。流结束后把本轮问答写入 Redis 会话记忆并异步落库 ai_message；需登录且会话须归当前用户所有。")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Validated @RequestBody ChatStreamRequest req) {
        return aiChatService.streamChat(req.getSessionId(), req.getQuestion(), req.getCode(), req.getLanguage());
    }

    @Operation(summary = "同步代码评审",
            description = "同步调用大模型对完整源码做评审，使用 review 场景提示词（{language}/{code}），返回 Markdown 评审文本。该接口不创建会话、不写会话消息，也不产生异步任务；需登录。")
    @PostMapping("/review")
    public Result<String> review(@Validated @RequestBody ReviewRequestDTO req) {
        return Result.ok(aiChatService.review(req));
    }

    @Operation(summary = "提交异步任务（出题/批量评审）",
            description = "校验 taskType（generate 出题 / review 代码评审）及对应必填参数后，生成 32 位 taskId，先把 PENDING 状态写入 Redis（保留 1 天），再投递 RocketMQ 异步执行；投递失败会删除状态。返回 taskId 供轮询查询；需登录。")
    @PostMapping("/task/submit")
    public Result<String> submitTask(@Validated @RequestBody TaskSubmitDTO req) {
        return Result.ok(aiTaskService.submitTask(req));
    }

    @Operation(summary = "查询异步任务状态与结果",
            description = "按 taskId 从 Redis 读取状态与结果：PENDING 待执行、SUCCESS 成功、FAILED 失败，结果保留 1 天。任务不存在或已过期返回错误；需登录。")
    @GetMapping("/task/{taskId}")
    public Result<TaskStatusVO> task(@Parameter(description = "异步任务 ID，即提交任务接口返回的 32 位 UUID（无连字符）", required = true, example = "e3b0c44298fc1c149afbf4c8996fb92427") @PathVariable("taskId") String taskId) {
        return Result.ok(aiTaskService.getTask(taskId));
    }
}
