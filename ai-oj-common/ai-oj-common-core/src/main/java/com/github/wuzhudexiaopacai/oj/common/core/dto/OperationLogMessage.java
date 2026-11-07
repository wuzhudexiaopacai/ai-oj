package com.github.wuzhudexiaopacai.oj.common.core.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志消息体（业务服务 → RocketMQ → 日志服务）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class OperationLogMessage implements Serializable {

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 来源服务名
     */
    private String serviceName;

    /**
     * 业务模块
     */
    private String module;

    /**
     * 操作描述
     */
    private String operation;

    /**
     * 操作类型：1新增 2修改 3删除 4查询 5导出 0其他
     */
    private Integer actionType;

    /**
     * HTTP 方法
     */
    private String httpMethod;

    /**
     * 请求路径
     */
    private String uri;

    /**
     * 类名#方法名
     */
    private String classMethod;

    /**
     * 操作人 ID
     */
    private Long userId;

    /**
     * 操作人用户名
     */
    private String username;

    /**
     * 客户端 IP
     */
    private String ip;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 请求入参（已脱敏并截断）
     */
    private String requestParam;

    /**
     * 响应出参（已脱敏并截断）
     */
    private String responseResult;

    /**
     * 是否成功：1成功 0失败
     */
    private Integer success;

    /**
     * 失败原因
     */
    private String errorMsg;

    /**
     * 耗时（毫秒）
     */
    private Integer costMs;

    /**
     * 操作时间
     */
    private LocalDateTime logTime;
}
