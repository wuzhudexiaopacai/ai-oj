-- =============================================================
-- AI-OJ 认证中心库（RBAC 权限模型）
-- @author wuzhudexiaopacai
-- =============================================================
CREATE DATABASE IF NOT EXISTS ai_oj_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_oj_auth;

SET NAMES utf8mb4;

-- -----------------------------
-- 用户表
-- -----------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id            BIGINT       NOT NULL COMMENT '用户ID（雪花）',
    username      VARCHAR(32)  NOT NULL COMMENT '登录名',
    nickname      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
    password      VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码哈希',
    email         VARCHAR(128) NOT NULL DEFAULT '' COMMENT '邮箱',
    avatar        VARCHAR(512) NOT NULL DEFAULT '' COMMENT '头像URL',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    intro         VARCHAR(512) NOT NULL DEFAULT '' COMMENT '个人简介',
    accept_count  INT          NOT NULL DEFAULT 0 COMMENT '通过题数',
    submit_count  INT          NOT NULL DEFAULT 0 COMMENT '提交总数',
    rating        INT          NOT NULL DEFAULT 1500 COMMENT '竞赛 Rating',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT       NULL COMMENT '创建人',
    update_by     BIGINT       NULL COMMENT '更新人',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email)
) ENGINE = InnoDB COMMENT '用户表';

-- -----------------------------
-- 角色表
-- -----------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id          BIGINT      NOT NULL COMMENT '角色ID',
    code        VARCHAR(64) NOT NULL COMMENT '角色编码，如 ROLE_ADMIN',
    name        VARCHAR(64) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) NOT NULL DEFAULT '' COMMENT '描述',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT      NULL,
    update_by   BIGINT      NULL,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB COMMENT '角色表';

-- -----------------------------
-- 权限表（菜单/按钮/API 三级）
-- -----------------------------
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    id          BIGINT       NOT NULL COMMENT '权限ID',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父权限ID，0为根',
    code        VARCHAR(128) NOT NULL COMMENT '权限标识，如 problem:create',
    name        VARCHAR(64)  NOT NULL COMMENT '权限名称',
    type        TINYINT      NOT NULL COMMENT '类型：1菜单 2按钮 3API',
    path        VARCHAR(255) NOT NULL DEFAULT '' COMMENT '路由路径',
    method      VARCHAR(8)   NOT NULL DEFAULT '' COMMENT 'HTTP方法（API类型用）',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_parent (parent_id)
) ENGINE = InnoDB COMMENT '权限表';

-- -----------------------------
-- 用户-角色关联
-- -----------------------------
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id          BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    role_id     BIGINT   NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT   NULL,
    update_by   BIGINT   NULL,
    deleted     TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE = InnoDB COMMENT '用户角色关联表';

-- -----------------------------
-- 角色-权限关联
-- -----------------------------
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    id            BIGINT   NOT NULL,
    role_id       BIGINT   NOT NULL,
    permission_id BIGINT   NOT NULL,
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by     BIGINT   NULL,
    update_by     BIGINT   NULL,
    deleted       TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE = InnoDB COMMENT '角色权限关联表';

-- -----------------------------
-- 登录日志（MQ 异步落库）
-- -----------------------------
DROP TABLE IF EXISTS sys_login_log;
CREATE TABLE sys_login_log (
    id          BIGINT       NOT NULL,
    user_id     BIGINT       NULL COMMENT '用户ID（登录失败可为空）',
    username    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '登录名',
    ip          VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '登录IP',
    user_agent  VARCHAR(512) NOT NULL DEFAULT '' COMMENT 'UA',
    success     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否成功',
    message     VARCHAR(255) NOT NULL DEFAULT '' COMMENT '附加信息',
    login_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_login_time (login_time)
) ENGINE = InnoDB COMMENT '登录日志表';
