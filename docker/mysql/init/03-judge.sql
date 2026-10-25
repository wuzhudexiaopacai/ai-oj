-- =============================================================
-- AI-OJ 判题库（提交记录 / 沙箱注册 / 死信归档 / Quartz 集群 / Seata undo_log）
-- @author wuzhudexiaopacai
-- =============================================================
CREATE DATABASE IF NOT EXISTS ai_oj_judge DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_oj_judge;

SET NAMES utf8mb4;

-- -----------------------------
-- 判题提交记录
-- -----------------------------
DROP TABLE IF EXISTS judge_submission;
CREATE TABLE judge_submission (
    id             BIGINT       NOT NULL COMMENT '提交记录ID',
    submit_id      VARCHAR(64)  NOT NULL COMMENT '幂等提交标识（客户端生成，唯一键防重）',
    user_id        BIGINT       NOT NULL COMMENT '提交用户',
    problem_id     BIGINT       NOT NULL COMMENT '题目ID',
    contest_id     BIGINT       NOT NULL DEFAULT 0 COMMENT '比赛ID，0表示非比赛提交',
    language       VARCHAR(32)  NOT NULL COMMENT '编程语言',
    code           MEDIUMTEXT   NOT NULL COMMENT '提交代码',
    code_length    INT          NOT NULL DEFAULT 0 COMMENT '代码长度',
    status         TINYINT      NOT NULL DEFAULT 0 COMMENT '判题状态：见 JudgeStatusEnum',
    score          INT          NOT NULL DEFAULT 0 COMMENT '得分',
    time_used      INT          NOT NULL DEFAULT 0 COMMENT '最大耗时(ms)',
    memory_used    INT          NOT NULL DEFAULT 0 COMMENT '最大内存(KB)',
    error_message  TEXT         NULL COMMENT '编译/运行错误信息',
    retry_count    INT          NOT NULL DEFAULT 0 COMMENT '业务重试次数',
    judge_time     DATETIME     NULL COMMENT '判题完成时间',
    version        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by      BIGINT       NULL,
    update_by      BIGINT       NULL,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_submit_id (submit_id),
    KEY idx_user_problem (user_id, problem_id),
    KEY idx_problem (problem_id),
    KEY idx_status (status),
    KEY idx_contest (contest_id)
) ENGINE = InnoDB COMMENT '判题提交表';

-- -----------------------------
-- 单用例判题结果明细
-- -----------------------------
DROP TABLE IF EXISTS judge_case_result;
CREATE TABLE judge_case_result (
    id             BIGINT      NOT NULL,
    submission_id  BIGINT      NOT NULL COMMENT '提交记录ID',
    seq            INT         NOT NULL COMMENT '用例序号',
    status         TINYINT     NOT NULL COMMENT '该用例状态码',
    time_used      INT         NOT NULL DEFAULT 0 COMMENT '耗时(ms)',
    memory_used    INT         NOT NULL DEFAULT 0 COMMENT '内存(KB)',
    score          INT         NOT NULL DEFAULT 0,
    output_preview VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '实际输出预览（截断）',
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by      BIGINT      NULL,
    update_by      BIGINT      NULL,
    deleted        TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_submission_seq (submission_id, seq)
) ENGINE = InnoDB COMMENT '判题用例结果表';

-- -----------------------------
-- 沙箱实例注册表（负载均衡与故障转移）
-- -----------------------------
DROP TABLE IF EXISTS judge_sandbox;
CREATE TABLE judge_sandbox (
    id            BIGINT      NOT NULL,
    sandbox_key   VARCHAR(64) NOT NULL COMMENT '沙箱唯一标识',
    base_url      VARCHAR(128) NOT NULL COMMENT '沙箱 HTTP 地址',
    max_parallel  INT         NOT NULL DEFAULT 4 COMMENT '最大并行判题数',
    status        TINYINT     NOT NULL DEFAULT 1 COMMENT '1在线 0离线',
    last_heartbeat DATETIME   NULL COMMENT '最后心跳时间',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by     BIGINT      NULL,
    update_by     BIGINT      NULL,
    deleted       TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sandbox_key (sandbox_key)
) ENGINE = InnoDB COMMENT '评测沙箱注册表';

INSERT INTO judge_sandbox (id, sandbox_key, base_url, max_parallel, status) VALUES
(1, 'sandbox-local-1', 'http://ai-oj-sandbox:8000', 4, 1);

-- -----------------------------
-- 比赛成绩记录（归属判题库：判题结果消费更新；报名时由比赛服务经 Seata 全局事务初始化）
-- -----------------------------
DROP TABLE IF EXISTS contest_record;
CREATE TABLE contest_record (
    id              BIGINT      NOT NULL,
    contest_id      BIGINT      NOT NULL,
    user_id         BIGINT      NOT NULL,
    username        VARCHAR(64) NOT NULL DEFAULT '' COMMENT '用户名快照（排行榜展示免回查）',
    total_score     INT         NOT NULL DEFAULT 0 COMMENT '总分',
    accept_count    INT         NOT NULL DEFAULT 0 COMMENT '通过题数',
    penalty_seconds BIGINT      NOT NULL DEFAULT 0 COMMENT 'ACM 罚时（秒）',
    rank_snapshot   INT         NULL COMMENT '结算名次快照',
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by       BIGINT      NULL,
    update_by       BIGINT      NULL,
    deleted         TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_contest_user (contest_id, user_id)
) ENGINE = InnoDB COMMENT '比赛成绩表';

-- -----------------------------
-- 业务死信归档表（DLQ 消费者落库，支持人工重放）
-- -----------------------------
DROP TABLE IF EXISTS biz_dead_letter;
CREATE TABLE biz_dead_letter (
    id             BIGINT       NOT NULL,
    topic          VARCHAR(128) NOT NULL COMMENT '原 Topic',
    consumer_group VARCHAR(128) NOT NULL COMMENT '原消费组',
    msg_id         VARCHAR(64)  NOT NULL COMMENT 'RocketMQ 消息ID',
    msg_body       MEDIUMTEXT   NOT NULL COMMENT '消息体',
    retry_times    INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
    status         TINYINT      NOT NULL DEFAULT 0 COMMENT '0待处理 1已重放 2已忽略',
    handle_remark  VARCHAR(512) NOT NULL DEFAULT '' COMMENT '处理备注',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by      BIGINT       NULL,
    update_by      BIGINT      NULL,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_msg_id (msg_id),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT '业务死信归档表';

-- -----------------------------
-- Seata AT 模式 undo_log
-- -----------------------------
DROP TABLE IF EXISTS undo_log;
CREATE TABLE undo_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    branch_id     BIGINT       NOT NULL,
    xid           VARCHAR(128) NOT NULL,
    context       VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB     NOT NULL,
    log_status    INT          NOT NULL,
    log_created   DATETIME     NOT NULL,
    log_modified  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB COMMENT 'Seata AT undo_log';

-- =============================================================
-- Quartz 集群模式持久化表（QRTZ_*）
-- =============================================================
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
CREATE TABLE QRTZ_JOB_DETAILS (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    JOB_NAME          VARCHAR(200) NOT NULL,
    JOB_GROUP         VARCHAR(200) NOT NULL,
    DESCRIPTION       VARCHAR(250) NULL,
    JOB_CLASS_NAME    VARCHAR(250) NOT NULL,
    IS_DURABLE        VARCHAR(1)   NOT NULL,
    IS_NONCONCURRENT  VARCHAR(1)   NOT NULL,
    IS_UPDATE_DATA    VARCHAR(1)   NOT NULL,
    REQUESTS_RECOVERY VARCHAR(1)   NOT NULL,
    JOB_DATA          BLOB         NULL,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
) ENGINE = InnoDB;

DROP TABLE IF EXISTS QRTZ_TRIGGERS;
CREATE TABLE QRTZ_TRIGGERS (
    SCHED_NAME     VARCHAR(120) NOT NULL,
    TRIGGER_NAME   VARCHAR(200) NOT NULL,
    TRIGGER_GROUP  VARCHAR(200) NOT NULL,
    JOB_NAME       VARCHAR(200) NOT NULL,
    JOB_GROUP      VARCHAR(200) NOT NULL,
    DESCRIPTION    VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT       NULL,
    PREV_FIRE_TIME BIGINT       NULL,
    PRIORITY       INTEGER      NULL,
    TRIGGER_STATE  VARCHAR(16)  NOT NULL,
    TRIGGER_TYPE   VARCHAR(8)   NOT NULL,
    START_TIME     BIGINT       NOT NULL,
    END_TIME       BIGINT       NULL,
    CALENDAR_NAME  VARCHAR(200) NULL,
    MISFIRE_INSTR  SMALLINT     NULL,
    JOB_DATA       BLOB         NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    KEY IDX_QRTZ_T_J (SCHED_NAME, JOB_NAME, JOB_GROUP),
    KEY IDX_QRTZ_T_JG (SCHED_NAME, JOB_GROUP),
    KEY IDX_QRTZ_T_C (SCHED_NAME, CALENDAR_NAME),
    KEY IDX_QRTZ_T_G (SCHED_NAME, TRIGGER_GROUP),
    KEY IDX_QRTZ_T_STATE (SCHED_NAME, TRIGGER_STATE),
    KEY IDX_QRTZ_T_N_STATE (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, TRIGGER_STATE),
    KEY IDX_QRTZ_T_N_G_STATE (SCHED_NAME, TRIGGER_GROUP, TRIGGER_STATE),
    KEY IDX_QRTZ_T_NEXT_FIRE_TIME (SCHED_NAME, NEXT_FIRE_TIME),
    KEY IDX_QRTZ_T_NFT_ST (SCHED_NAME, TRIGGER_STATE, NEXT_FIRE_TIME),
    KEY IDX_QRTZ_T_NFT_MISFIRE (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME),
    KEY IDX_QRTZ_T_NFT_ST_MISFIRE (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE),
    KEY IDX_QRTZ_T_NFT_ST_MISFIRE_GRP (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE, TRIGGER_GROUP)
) ENGINE = InnoDB;

DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    REPEAT_COUNT    BIGINT       NOT NULL,
    REPEAT_INTERVAL BIGINT       NOT NULL,
    TIMES_TRIGGERED BIGINT       NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE = InnoDB;

DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
CREATE TABLE QRTZ_CRON_TRIGGERS (
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID    VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE = InnoDB;

DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
CREATE TABLE QRTZ_SIMPROP_TRIGGERS (
    SCHED_NAME    VARCHAR(120)   NOT NULL,
    TRIGGER_NAME  VARCHAR(200)   NOT NULL,
    TRIGGER_GROUP VARCHAR(200)   NOT NULL,
    STR_PROP_1    VARCHAR(512)   NULL,
    STR_PROP_2    VARCHAR(512)   NULL,
    STR_PROP_3    VARCHAR(512)   NULL,
    INT_PROP_1    INT            NULL,
    INT_PROP_2    INT            NULL,
    LONG_PROP_1   BIGINT         NULL,
    LONG_PROP_2   BIGINT         NULL,
    DEC_PROP_1    NUMERIC(13, 4) NULL,
    DEC_PROP_2    NUMERIC(13, 4) NULL,
    BOOL_PROP_1   VARCHAR(1)     NULL,
    BOOL_PROP_2   VARCHAR(1)     NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE = InnoDB;

DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
CREATE TABLE QRTZ_BLOB_TRIGGERS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_NAME  VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    BLOB_DATA     BLOB         NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE = InnoDB;

DROP TABLE IF EXISTS QRTZ_CALENDARS;
CREATE TABLE QRTZ_CALENDARS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(200) NOT NULL,
    CALENDAR      BLOB         NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
) ENGINE = InnoDB;

DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
) ENGINE = InnoDB;

DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
CREATE TABLE QRTZ_FIRED_TRIGGERS (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    ENTRY_ID          VARCHAR(95)  NOT NULL,
    TRIGGER_NAME      VARCHAR(200) NOT NULL,
    TRIGGER_GROUP     VARCHAR(200) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    FIRED_TIME        BIGINT       NOT NULL,
    SCHED_TIME        BIGINT       NOT NULL,
    PRIORITY          INTEGER      NOT NULL,
    STATE             VARCHAR(16)  NOT NULL,
    JOB_NAME          VARCHAR(200) NULL,
    JOB_GROUP         VARCHAR(200) NULL,
    IS_NONCONCURRENT  VARCHAR(1)   NULL,
    REQUESTS_RECOVERY VARCHAR(1)   NULL,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID),
    KEY IDX_QRTZ_FT_TRIG_INST_NAME (SCHED_NAME, INSTANCE_NAME),
    KEY IDX_QRTZ_FT_INST_JOB_REQ_RCVRY (SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY),
    KEY IDX_QRTZ_FT_J_G (SCHED_NAME, JOB_NAME, JOB_GROUP),
    KEY IDX_QRTZ_FT_JG (SCHED_NAME, JOB_GROUP),
    KEY IDX_QRTZ_FT_T_G (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    KEY IDX_QRTZ_FT_TG (SCHED_NAME, TRIGGER_GROUP)
) ENGINE = InnoDB;

DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
CREATE TABLE QRTZ_SCHEDULER_STATE (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT       NOT NULL,
    CHECKIN_INTERVAL  BIGINT       NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
) ENGINE = InnoDB;

DROP TABLE IF EXISTS QRTZ_LOCKS;
CREATE TABLE QRTZ_LOCKS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40)  NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
) ENGINE = InnoDB;
