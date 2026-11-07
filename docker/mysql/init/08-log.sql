-- ============================================================
-- AI-OJ 统一日志服务库：操作日志（业务写操作、入参、出参、耗时）
-- @author wuzhudexiaopacai
-- ============================================================
CREATE DATABASE IF NOT EXISTS `ai_oj_log` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `ai_oj_log`;

CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id`              BIGINT        NOT NULL COMMENT '主键（雪花 ID）',
    `trace_id`        VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '链路追踪 ID',
    `service_name`    VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '来源服务名',
    `module`          VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '业务模块',
    `operation`       VARCHAR(128)  NOT NULL DEFAULT '' COMMENT '操作描述',
    `action_type`     TINYINT       NOT NULL DEFAULT 0 COMMENT '操作类型：1新增 2修改 3删除 4查询 5导出 0其他',
    `http_method`     VARCHAR(8)    NOT NULL DEFAULT '' COMMENT 'HTTP 方法',
    `uri`             VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '请求路径',
    `class_method`    VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '类名#方法名',
    `user_id`         BIGINT        NULL COMMENT '操作人 ID',
    `username`        VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '操作人用户名',
    `ip`              VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '客户端 IP',
    `user_agent`      VARCHAR(512)  NOT NULL DEFAULT '' COMMENT 'User-Agent',
    `request_param`   MEDIUMTEXT    NULL COMMENT '请求入参（脱敏并截断）',
    `response_result` MEDIUMTEXT    NULL COMMENT '响应出参（脱敏并截断）',
    `success`         TINYINT       NOT NULL DEFAULT 1 COMMENT '是否成功：1成功 0失败',
    `error_msg`       VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '失败原因',
    `cost_ms`         INT           NOT NULL DEFAULT 0 COMMENT '耗时（毫秒）',
    `log_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       BIGINT        NULL COMMENT '创建人',
    `update_by`       BIGINT        NULL COMMENT '更新人',
    `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_log_time` (`log_time`),
    KEY `idx_user_time` (`user_id`, `log_time`),
    KEY `idx_uri` (`uri`(64)),
    KEY `idx_module` (`module`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '统一操作日志表';
