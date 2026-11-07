package com.github.wuzhudexiaopacai.oj.log.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 操作日志分页查询条件
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "操作日志分页查询条件")
public class OperationLogQuery {

    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10", defaultValue = "10")
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Integer pageSize = 10;

    @Schema(description = "业务模块（模糊匹配）", example = "题目管理")
    private String module;

    @Schema(description = "操作描述（模糊匹配）", example = "新建题目")
    private String operation;

    @Schema(description = "操作人用户名（模糊匹配）", example = "admin")
    private String username;

    @Schema(description = "是否成功：1成功 0失败", example = "1")
    private Integer success;

    @Schema(description = "关键字（匹配请求路径或类方法）", example = "problem")
    private String keyword;

    @Schema(description = "开始时间", example = "2026-01-01 00:00:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2026-12-31 23:59:59")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
