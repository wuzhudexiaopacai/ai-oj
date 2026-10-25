-- =============================================================
-- AI-OJ 比赛库
-- @author wuzhudexiaopacai
-- =============================================================
CREATE DATABASE IF NOT EXISTS ai_oj_contest DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_oj_contest;

SET NAMES utf8mb4;

-- -----------------------------
-- 比赛表（状态由状态机驱动）
-- -----------------------------
DROP TABLE IF EXISTS contest;
CREATE TABLE contest (
    id             BIGINT       NOT NULL,
    title          VARCHAR(128) NOT NULL COMMENT '比赛标题',
    description    MEDIUMTEXT   NULL COMMENT '比赛说明（Markdown）',
    type           TINYINT      NOT NULL DEFAULT 0 COMMENT '赛制：0 ACM 1 OI',
    status         VARCHAR(16)  NOT NULL DEFAULT 'SCHEDULED' COMMENT '状态机：SCHEDULED/RUNNING/ENDED/CANCELED',
    start_time     DATETIME     NOT NULL,
    end_time       DATETIME     NOT NULL,
    seal_time      DATETIME     NULL COMMENT '封榜时间',
    creator_id     BIGINT       NOT NULL COMMENT '创建者',
    password       VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '私有赛密码（空为公开）',
    auth           TINYINT      NOT NULL DEFAULT 1 COMMENT '1公开 2私有',
    version        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by      BIGINT       NULL,
    update_by      BIGINT       NULL,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_status_time (status, start_time),
    KEY idx_start_time (start_time)
) ENGINE = InnoDB COMMENT '比赛表';

-- -----------------------------
-- 比赛题目（display_id 为赛中题号 A/B/C...）
-- -----------------------------
DROP TABLE IF EXISTS contest_problem;
CREATE TABLE contest_problem (
    id          BIGINT      NOT NULL,
    contest_id  BIGINT      NOT NULL,
    problem_id  BIGINT      NOT NULL,
    display_id  VARCHAR(8)  NOT NULL COMMENT '赛中题号，如 A',
    title       VARCHAR(128) NOT NULL DEFAULT '' COMMENT '赛中展示标题（可覆盖原题）',
    score       INT         NOT NULL DEFAULT 0 COMMENT 'OI 模式分值',
    sort        INT         NOT NULL DEFAULT 0,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT      NULL,
    update_by   BIGINT      NULL,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_contest_display (contest_id, display_id),
    KEY idx_contest (contest_id)
) ENGINE = InnoDB COMMENT '比赛题目表';

-- -----------------------------
-- 比赛报名（唯一键幂等防重复报名）
-- -----------------------------
DROP TABLE IF EXISTS contest_registration;
CREATE TABLE contest_registration (
    id          BIGINT   NOT NULL,
    contest_id  BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    status      TINYINT  NOT NULL DEFAULT 1 COMMENT '1已报名 0已取消',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT   NULL,
    update_by   BIGINT   NULL,
    deleted     TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_contest_user (contest_id, user_id)
) ENGINE = InnoDB COMMENT '比赛报名表';

-- 注：比赛成绩记录 contest_record 归属判题库 ai_oj_judge（由判题结果驱动更新，
--     报名时通过 Seata 全局事务在判题服务侧初始化，保证与报名记录同生共死）

-- -----------------------------
-- Seata AT 模式 undo_log（contest 为分布式事务发起方）
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
