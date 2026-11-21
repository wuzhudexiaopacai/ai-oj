-- =============================================================
-- AI-OJ 题库库
-- @author wuzhudexiaopacai
-- =============================================================
CREATE DATABASE IF NOT EXISTS ai_oj_problem DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_oj_problem;

SET NAMES utf8mb4;

-- -----------------------------
-- 题目表
-- -----------------------------
DROP TABLE IF EXISTS pms_problem;
CREATE TABLE pms_problem (
    id             BIGINT       NOT NULL COMMENT '题目ID（雪花）',
    problem_key    VARCHAR(32)  NOT NULL COMMENT '展示题号，如 A1001',
    title          VARCHAR(128) NOT NULL COMMENT '题目标题',
    description    MEDIUMTEXT   NOT NULL COMMENT '题目描述（Markdown）',
    input_desc     TEXT         NULL COMMENT '输入描述',
    output_desc    TEXT         NULL COMMENT '输出描述',
    hint           TEXT         NULL COMMENT '提示',
    source         VARCHAR(128) NOT NULL DEFAULT '' COMMENT '来源',
    difficulty     TINYINT      NOT NULL DEFAULT 0 COMMENT '难度：0简单 1中等 2困难（与 ProblemDifficultyEnum 一致）',
    time_limit     INT          NOT NULL DEFAULT 1000 COMMENT '时间限制(ms)',
    memory_limit   INT          NOT NULL DEFAULT 256 COMMENT '内存限制(MB)',
    stack_limit    INT          NOT NULL DEFAULT 128 COMMENT '栈限制(MB)',
    is_spj         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否特殊判题：0否 1是',
    spj_code       MEDIUMTEXT   NULL COMMENT 'SPJ 判题代码',
    spj_language   VARCHAR(32)  NULL COMMENT 'SPJ 语言',
    io_score       TINYINT      NOT NULL DEFAULT 0 COMMENT '计分模式：0 ACM(全对才得分) 1 OI(按点给分)',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0下架 1上架 2比赛中',
    auth           TINYINT      NOT NULL DEFAULT 1 COMMENT '可见性：1公开 2私有 3比赛',
    accept_count   INT          NOT NULL DEFAULT 0 COMMENT '通过数（MQ 异步统计）',
    submit_count   INT          NOT NULL DEFAULT 0 COMMENT '提交数（MQ 异步统计）',
    version        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by      BIGINT       NULL,
    update_by      BIGINT       NULL,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_problem_key (problem_key),
    KEY idx_difficulty_status (difficulty, status),
    KEY idx_title (title)
) ENGINE = InnoDB COMMENT '题目表';

-- -----------------------------
-- 标签分类
-- -----------------------------
DROP TABLE IF EXISTS pms_tag_category;
CREATE TABLE pms_tag_category (
    id          BIGINT      NOT NULL,
    name        VARCHAR(32) NOT NULL COMMENT '分类名，如 算法/数据结构',
    sort        INT         NOT NULL DEFAULT 0,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT      NULL,
    update_by   BIGINT      NULL,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE = InnoDB COMMENT '标签分类表';

-- -----------------------------
-- 标签
-- -----------------------------
DROP TABLE IF EXISTS pms_tag;
CREATE TABLE pms_tag (
    id          BIGINT      NOT NULL,
    category_id BIGINT      NOT NULL COMMENT '分类ID',
    name        VARCHAR(32) NOT NULL COMMENT '标签名',
    color       VARCHAR(16) NOT NULL DEFAULT '' COMMENT '展示颜色',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT      NULL,
    update_by   BIGINT      NULL,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (category_id, name)
) ENGINE = InnoDB COMMENT '标签表';

-- -----------------------------
-- 题目-标签关联
-- -----------------------------
DROP TABLE IF EXISTS pms_problem_tag;
CREATE TABLE pms_problem_tag (
    id          BIGINT   NOT NULL,
    problem_id  BIGINT   NOT NULL,
    tag_id      BIGINT   NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT   NULL,
    update_by   BIGINT   NULL,
    deleted     TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_problem_tag (problem_id, tag_id),
    KEY idx_tag (tag_id)
) ENGINE = InnoDB COMMENT '题目标签关联表';

-- -----------------------------
-- 评测用例（大文件走 MinIO）
-- -----------------------------
DROP TABLE IF EXISTS pms_test_case;
CREATE TABLE pms_test_case (
    id          BIGINT      NOT NULL,
    problem_id  BIGINT      NOT NULL COMMENT '题目ID',
    seq         INT         NOT NULL COMMENT '用例序号',
    input_data  MEDIUMTEXT  NULL COMMENT '输入（小数据直接存库）',
    output_data MEDIUMTEXT  NULL COMMENT '期望输出（小数据直接存库）',
    input_file  VARCHAR(255) NULL COMMENT 'MinIO 输入文件对象名（大数据）',
    output_file VARCHAR(255) NULL COMMENT 'MinIO 输出文件对象名（大数据）',
    score       INT         NOT NULL DEFAULT 0 COMMENT 'OI 模式该用例分值',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT      NULL,
    update_by   BIGINT      NULL,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_problem (problem_id)
) ENGINE = InnoDB COMMENT '评测用例表';

-- -----------------------------
-- 题目多语言代码模板
-- -----------------------------
DROP TABLE IF EXISTS pms_problem_language;
CREATE TABLE pms_problem_language (
    id           BIGINT      NOT NULL,
    problem_id   BIGINT      NOT NULL,
    language     VARCHAR(32) NOT NULL COMMENT '语言：java/cpp/python/go...',
    code_template TEXT       NULL COMMENT '初始代码模板',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by    BIGINT      NULL,
    update_by    BIGINT      NULL,
    deleted      TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_problem_lang (problem_id, language)
) ENGINE = InnoDB COMMENT '题目语言模板表';

-- -----------------------------
-- 题解表
-- -----------------------------
DROP TABLE IF EXISTS pms_solution;
CREATE TABLE pms_solution (
    id          BIGINT      NOT NULL,
    problem_id  BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL COMMENT '作者',
    title       VARCHAR(128) NOT NULL,
    content     MEDIUMTEXT  NOT NULL COMMENT 'Markdown 内容',
    like_count  INT         NOT NULL DEFAULT 0,
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1正常 0待审核',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT      NULL,
    update_by   BIGINT      NULL,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_problem (problem_id)
) ENGINE = InnoDB COMMENT '题解表';

-- -----------------------------
-- 题解点赞关系表（物理删除，唯一键防重复点赞）
-- -----------------------------
DROP TABLE IF EXISTS pms_solution_like;
CREATE TABLE pms_solution_like (
    id          BIGINT   NOT NULL,
    solution_id BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_solution_user (solution_id, user_id),
    KEY idx_user (user_id)
) ENGINE = InnoDB COMMENT '题解点赞关系表';

-- ============================= 种子数据 =============================
-- 标签分类
INSERT INTO pms_tag_category (id, name, sort) VALUES
(1, '算法', 1),
(2, '数据结构', 2);

-- 标签
INSERT INTO pms_tag (id, category_id, name, color) VALUES
(11, 1, '数组', '#3b82f6'),
(12, 1, '哈希表', '#8b5cf6'),
(13, 1, '动态规划', '#f59e0b'),
(14, 2, '链表', '#10b981'),
(15, 2, '二叉树', '#06b6d4');

-- 示例题：两数之和
INSERT INTO pms_problem (id, problem_key, title, description, input_desc, output_desc, hint, difficulty,
                         time_limit, memory_limit, io_score, status, auth) VALUES
(1001, 'A1001', '两数之和',
'## 题目描述\n\n给定一个整数数组 `nums` 和一个整数目标值 `target`，请你在该数组中找出**和为目标值** `target` 的那**两个**整数，并返回它们的数组下标。\n\n你可以假设每种输入只会对应一个答案，并且不能使用两次相同的元素。\n\n## 示例\n\n**输入：** `nums = [2,7,11,15], target = 9`\n\n**输出：** `[0,1]`\n\n**解释：** 因为 `nums[0] + nums[1] == 9`，返回 `[0, 1]`。',
'第一行两个整数 n 和 target；第二行 n 个整数表示数组 nums。',
'输出两个下标，空格分隔。',
'可以使用哈希表将时间复杂度优化到 O(n)。',
0, 1000, 256, 0, 1, 1);

-- 用例
INSERT INTO pms_test_case (id, problem_id, seq, input_data, output_data, score) VALUES
(10011, 1001, 1, '4 9\n2 7 11 15', '0 1', 0),
(10012, 1001, 2, '3 6\n3 2 4', '1 2', 0);

-- 语言模板
INSERT INTO pms_problem_language (id, problem_id, language, code_template) VALUES
(10021, 1001, 'java', 'import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // TODO: 读取输入并求解\n    }\n}'),
(10022, 1001, 'cpp', '#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    // TODO: 读取输入并求解\n    return 0;\n}'),
(10023, 1001, 'python', 'import sys\n\n# TODO: 读取输入并求解\n'),
(10024, 1001, 'go', 'package main\n\nimport "fmt"\n\nfunc main() {\n\t// TODO: 读取输入并求解\n\t_ = fmt.Sprint()\n}');
