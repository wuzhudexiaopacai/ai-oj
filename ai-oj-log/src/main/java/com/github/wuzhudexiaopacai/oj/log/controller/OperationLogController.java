package com.github.wuzhudexiaopacai.oj.log.controller;

import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.log.dto.OperationLogQuery;
import com.github.wuzhudexiaopacai.oj.log.service.OperationLogService;
import com.github.wuzhudexiaopacai.oj.log.vo.OperationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询接口
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "操作日志", description = "统一操作日志查询，仅管理员可访问")
@RestController
@RequestMapping("/admin/log")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询操作日志", description = "支持按模块、操作人、成功状态、关键字与时间范围筛选，按操作时间倒序")
    @GetMapping("/operation/page")
    @PreAuthorize("hasAuthority('log:operation:list')")
    public Result<PageResult<OperationLogVO>> page(@Parameter(description = "查询条件") @Valid OperationLogQuery query) {
        return Result.ok(operationLogService.pageLog(query));
    }
}
