package com.github.wuzhudexiaopacai.oj.judge.controller;

import com.github.wuzhudexiaopacai.oj.common.core.enums.OpType;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.annotation.OperationLog;
import com.github.wuzhudexiaopacai.oj.judge.service.JudgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端提交管理接口（权限码：judge:submission:rejudge）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "提交管理接口", description = "管理端提交操作；需登录并具备 judge:submission:rejudge 权限码")
@RestController
@RequestMapping("/admin/submission")
@RequiredArgsConstructor
public class AdminSubmissionController {

    private final JudgeService judgeService;

    @Operation(summary = "重判提交", description = "将指定提交重置为排队中并清空上一轮得分、耗时、内存、错误信息与用例明细，随后重新投递判题消息。适用于题目用例/SPJ/限制变更后重新评测历史提交。需 judge:submission:rejudge 权限。")
    @OperationLog(module = "提交管理", operation = "重判提交", type = OpType.UPDATE)
    @PostMapping("/{id}/rejudge")
    @PreAuthorize("hasAuthority('judge:submission:rejudge')")
    public Result<Void> rejudge(
            @PathVariable("id")
            @Parameter(description = "提交记录主键 ID（雪花 ID）", required = true,
                    example = "1950000000000000001") Long id) {
        judgeService.rejudge(id);
        return Result.ok();
    }
}
