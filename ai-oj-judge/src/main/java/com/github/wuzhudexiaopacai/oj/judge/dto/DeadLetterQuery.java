package com.github.wuzhudexiaopacai.oj.judge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 死信分页查询参数
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "死信分页查询参数")
public class DeadLetterQuery {

    @Min(1)
    @Schema(description = "页码，从 1 开始", example = "1")
    private long pageNum = 1;

    @Min(1)
    @Max(100)
    @Schema(description = "每页条数，1-100", example = "10")
    private long pageSize = 10;

    @Schema(description = "状态：0 待处理、1 已重放、2 已忽略；不传则全部", example = "0")
    private Integer status;

    @Schema(description = "关键字，匹配消息 ID、原 Topic 或消费组", example = "AC1600")
    private String keyword;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "归档时间下界，格式 yyyy-MM-dd HH:mm:ss", example = "2026-01-01 00:00:00")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "归档时间上界，格式 yyyy-MM-dd HH:mm:ss", example = "2026-12-31 23:59:59")
    private LocalDateTime endTime;
}
