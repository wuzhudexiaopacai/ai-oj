package com.github.wuzhudexiaopacai.oj.ai.controller;

import com.github.wuzhudexiaopacai.oj.ai.dto.TemplateUpdateDTO;
import com.github.wuzhudexiaopacai.oj.ai.service.PromptTemplateService;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 管理接口（提示词模板热更新）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "AI 管理接口")
@RestController
@RequestMapping("/ai/admin")
@RequiredArgsConstructor
public class AiAdminController {

    private final PromptTemplateService promptTemplateService;

    @Operation(summary = "更新提示词模板（改库后失效缓存）")
    @PreAuthorize("hasAuthority('ai:template')")
    @PutMapping("/template/{code}")
    public Result<Void> updateTemplate(@PathVariable("code") String code,
                                       @Validated @RequestBody TemplateUpdateDTO dto) {
        promptTemplateService.updateTemplate(code, dto.getName(), dto.getTemplate());
        return Result.ok();
    }
}
