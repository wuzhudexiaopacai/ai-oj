package com.github.wuzhudexiaopacai.oj.log.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志视图对象
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "操作日志")
public class OperationLogVO {

    @Schema(description = "主键 ID", example = "1789000000000000001")
    private Long id;

    @Schema(description = "链路追踪 ID", example = "a1b2c3d4e5f6a7b8")
    private String traceId;

    @Schema(description = "来源服务", example = "ai-oj-problem")
    private String serviceName;

    @Schema(description = "业务模块", example = "题目管理")
    private String module;

    @Schema(description = "操作描述", example = "新建题目")
    private String operation;

    @Schema(description = "操作类型", example = "1")
    private Integer actionType;

    @Schema(description = "操作类型名称", example = "新增")
    private String actionTypeLabel;

    @Schema(description = "HTTP 方法", example = "POST")
    private String httpMethod;

    @Schema(description = "请求路径", example = "/admin/problem")
    private String uri;

    @Schema(description = "类名#方法名", example = "ProblemAdminController#create")
    private String classMethod;

    @Schema(description = "操作人 ID", example = "1")
    private Long userId;

    @Schema(description = "操作人用户名", example = "admin")
    private String username;

    @Schema(description = "客户端 IP", example = "172.18.0.1")
    private String ip;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "请求入参（脱敏截断后）")
    private String requestParam;

    @Schema(description = "响应出参（脱敏截断后）")
    private String responseResult;

    @Schema(description = "是否成功：1成功 0失败", example = "1")
    private Integer success;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "耗时（毫秒）", example = "12")
    private Integer costMs;

    @Schema(description = "操作时间")
    private LocalDateTime logTime;
}
