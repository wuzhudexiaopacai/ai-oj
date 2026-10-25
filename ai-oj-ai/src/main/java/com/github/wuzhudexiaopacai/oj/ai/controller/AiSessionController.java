package com.github.wuzhudexiaopacai.oj.ai.controller;

import com.github.wuzhudexiaopacai.oj.ai.dto.SessionCreateDTO;
import com.github.wuzhudexiaopacai.oj.ai.service.AiSessionService;
import com.github.wuzhudexiaopacai.oj.ai.vo.MessageVO;
import com.github.wuzhudexiaopacai.oj.ai.vo.SessionVO;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "AI 会话接口")
@RestController
@RequestMapping("/ai/session")
@RequiredArgsConstructor
public class AiSessionController {

    private final AiSessionService aiSessionService;

    @Operation(summary = "创建会话")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody SessionCreateDTO dto) {
        return Result.ok(aiSessionService.createSession(dto));
    }

    @Operation(summary = "我的会话分页")
    @GetMapping("/page")
    public Result<PageResult<SessionVO>> page(@RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
                                              @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.ok(aiSessionService.pageMySessions(pageNum, pageSize));
    }

    @Operation(summary = "会话消息列表")
    @GetMapping("/{id}/messages")
    public Result<List<MessageVO>> messages(@PathVariable("id") Long id) {
        return Result.ok(aiSessionService.listMessages(id));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        aiSessionService.deleteSession(id);
        return Result.ok();
    }
}
