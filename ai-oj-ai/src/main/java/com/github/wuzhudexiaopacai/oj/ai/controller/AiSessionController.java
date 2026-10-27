package com.github.wuzhudexiaopacai.oj.ai.controller;

import com.github.wuzhudexiaopacai.oj.ai.dto.SessionCreateDTO;
import com.github.wuzhudexiaopacai.oj.ai.service.AiSessionService;
import com.github.wuzhudexiaopacai.oj.ai.vo.MessageVO;
import com.github.wuzhudexiaopacai.oj.ai.vo.SessionVO;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 会话接口
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "AI 会话接口", description = "AI 会话创建、分页、消息查询与删除；经网关 JWT 鉴权，所有接口需登录且只能操作本人会话")
@RestController
@RequestMapping("/ai/session")
@RequiredArgsConstructor
public class AiSessionController {

    private final AiSessionService aiSessionService;

    @Operation(summary = "创建会话",
            description = "校验 scene 是否为受支持场景（chat/hint/review/generate）后，创建归属当前登录用户的会话；title 为空时按场景生成默认标题，problemId 可为空表示通用对话。返回雪花会话 ID（JSON 序列化为字符串）。")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody SessionCreateDTO dto) {
        return Result.ok(aiSessionService.createSession(dto));
    }

    @Operation(summary = "我的会话分页",
            description = "分页查询当前登录用户自己的会话，按更新时间倒序；pageNum 从 1 开始，pageSize 默认 10，不返回他人会话。")
    @GetMapping("/page")
    public Result<PageResult<SessionVO>> page(@Parameter(description = "页码，从 1 开始", required = false, example = "1") @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
                                              @Parameter(description = "每页条数，默认 10", required = false, example = "10") @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.ok(aiSessionService.pageMySessions(pageNum, pageSize));
    }

    @Operation(summary = "会话消息列表",
            description = "先校验会话归属当前用户，再按消息 ID 升序返回该会话全部消息（user 提问与 assistant 回答）。流式回答在结束后异步落库，刚完成对话时列表可能略有延迟。")
    @GetMapping("/{id}/messages")
    public Result<List<MessageVO>> messages(@Parameter(description = "会话 ID（雪花 ID）", required = true, example = "1850000000000000001") @PathVariable("id") Long id) {
        return Result.ok(aiSessionService.listMessages(id));
    }

    @Operation(summary = "删除会话",
            description = "校验会话归属当前用户后，删除会话记录及其全部消息，并清理 Redis 中的会话记忆；删除不可恢复。")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "会话 ID（雪花 ID）", required = true, example = "1850000000000000001") @PathVariable("id") Long id) {
        aiSessionService.deleteSession(id);
        return Result.ok();
    }
}
