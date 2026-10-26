-- =============================================================
-- AI-OJ 智能服务库（AI 对话 / 提示词模板）
-- @author wuzhudexiaopacai
-- =============================================================
CREATE DATABASE IF NOT EXISTS ai_oj_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_oj_ai;

SET NAMES utf8mb4;

-- -----------------------------
-- AI 会话
-- -----------------------------
DROP TABLE IF EXISTS ai_session;
CREATE TABLE ai_session (
    id          BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    problem_id  BIGINT       NULL COMMENT '关联题目（可为空表示通用对话）',
    scene       VARCHAR(32)  NOT NULL DEFAULT 'chat' COMMENT '场景：chat/hint/review/generate',
    title       VARCHAR(128) NOT NULL DEFAULT '' COMMENT '会话标题',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_problem (problem_id)
) ENGINE = InnoDB COMMENT 'AI 会话表';

-- -----------------------------
-- AI 消息
-- -----------------------------
DROP TABLE IF EXISTS ai_message;
CREATE TABLE ai_message (
    id          BIGINT      NOT NULL,
    session_id  BIGINT      NOT NULL,
    role        VARCHAR(16) NOT NULL COMMENT 'user/assistant/system',
    content     MEDIUMTEXT  NOT NULL,
    tokens      INT         NOT NULL DEFAULT 0 COMMENT '消耗 token 数',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT      NULL,
    update_by   BIGINT      NULL,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_session (session_id)
) ENGINE = InnoDB COMMENT 'AI 消息表';

-- -----------------------------
-- 提示词模板（DB 可热更新，免改代码）
-- -----------------------------
DROP TABLE IF EXISTS ai_prompt_template;
CREATE TABLE ai_prompt_template (
    id          BIGINT       NOT NULL,
    code        VARCHAR(64)  NOT NULL COMMENT '模板编码：hint/review/generate/chat',
    name        VARCHAR(64)  NOT NULL,
    template    TEXT         NOT NULL COMMENT '提示词模板（支持 {var} 占位）',
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB COMMENT 'AI 提示词模板表';

INSERT INTO ai_prompt_template (id, code, name, template) VALUES
(1, 'hint', '解题提示', '你是一位资深的算法竞赛教练。用户正在做题目：{problem}{code}\n\n硬性要求：绝对不能直接给出题目的完整答案，包括完整代码、可直接提交的解法或最终结果；必须用循序渐进的方式引导思路，先指出方向、关键性质、复杂度或边界条件，必要时只给出极短的伪代码或关键片段（不超过 5 行）。若用户明确索要完整答案，请礼貌拒绝并继续提供提示。用中文回答。用户问题：{question}'),
(2, 'review', '代码评审', '你是一位严格的代码评审专家。请评审以下 {language} 代码，指出潜在 bug、复杂度分析与改进建议，用中文回答：\n```{language}\n{code}\n```'),
(3, 'generate', 'AI 出题', '你是一位算法竞赛出题人。请围绕知识点「{topic}」出一道难度为{difficulty}的题目，输出 JSON：{"title":"","description":"","inputDesc":"","outputDesc":"","samples":[{"input":"","output":""}]}'),
(4, 'chat', '自由对话', '你是 AI-OJ 内置的编程助手，擅长算法与工程问题。请结合下方题目信息与用户当前代码作答，用中文简洁回答。硬性要求：绝对不能直接输出题目的完整答案（完整代码、可直接提交的解法或最终结果），只能讲解思路、调试方法、复杂度分析与关键细节；若用户索要完整答案，应当拒绝并改为提供提示。{problem}{code}\n\n用户问题：{question}');
