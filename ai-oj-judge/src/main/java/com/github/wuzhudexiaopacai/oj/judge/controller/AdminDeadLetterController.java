package com.github.wuzhudexiaopacai.oj.judge.controller;

import com.github.wuzhudexiaopacai.oj.common.core.enums.OpType;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.annotation.OperationLog;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.judge.dto.DeadLetterQuery;
import com.github.wuzhudexiaopacai.oj.judge.service.DeadLetterService;
import com.github.wuzhudexiaopacai.oj.judge.vo.DeadLetterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端死信归档接口（权限码：judge:deadletter:list / judge:deadletter:handle）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "死信管理接口", description = "判题死信归档的分页查询、重放与忽略；需登录并具备 judge:deadletter:list / judge:deadletter:handle 权限码")
@RestController
@RequestMapping("/admin/dead-letter")
@RequiredArgsConstructor
public class AdminDeadLetterController {

    private final DeadLetterService deadLetterService;

    @Operation(summary = "死信分页", description = "分页查询判题死信归档记录，按归档时间倒序；支持状态（0待处理 1已重放 2已忽略）、关键字（消息 ID/Topic/消费组）与时间范围筛选。需 judge:deadletter:list 权限。")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('judge:deadletter:list')")
    public Result<PageResult<DeadLetterVO>> page(@ParameterObject DeadLetterQuery query) {
        return Result.ok(deadLetterService.page(query));
    }

    @Operation(summary = "重放死信", description = "将待处理死信的消息体重新投递到原 Topic，成功后标记为已重放并累加重试次数；仅状态为待处理（0）的记录可重放，已处理记录返回业务失败。需 judge:deadletter:handle 权限。")
    @OperationLog(module = "死信管理", operation = "重放死信", type = OpType.UPDATE)
    @PostMapping("/{id}/replay")
    @PreAuthorize("hasAuthority('judge:deadletter:handle')")
    public Result<Void> replay(
            @PathVariable("id")
            @Parameter(description = "死信记录 ID", required = true, example = "1960000000000000001") Long id,
            @RequestParam(value = "remark", required = false)
            @Parameter(description = "处理备注，最长 512 字", example = "沙箱恢复后重放") String remark) {
        deadLetterService.replay(id, remark);
        return Result.ok();
    }

    @Operation(summary = "忽略死信", description = "将待处理死信标记为已忽略（不重新投递），仅状态为待处理（0）的记录可忽略，已处理记录返回业务失败。需 judge:deadletter:handle 权限。")
    @OperationLog(module = "死信管理", operation = "忽略死信", type = OpType.UPDATE)
    @PostMapping("/{id}/ignore")
    @PreAuthorize("hasAuthority('judge:deadletter:handle')")
    public Result<Void> ignore(
            @PathVariable("id")
            @Parameter(description = "死信记录 ID", required = true, example = "1960000000000000001") Long id,
            @RequestParam(value = "remark", required = false)
            @Parameter(description = "处理备注，最长 512 字", example = "重复消息，无需重放") String remark) {
        deadLetterService.ignore(id, remark);
        return Result.ok();
    }
}
