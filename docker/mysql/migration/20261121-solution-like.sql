-- =============================================================
-- 题解点赞关系落库：原点赞关系仅存 Redis，重启/淘汰后丢失且计数会漂移
-- 适用场景：已初始化过的库（docker/mysql/init 只在首次建卷时执行）
-- 全新部署无需执行，02-problem.sql 已包含该表
-- =============================================================
USE ai_oj_problem;

CREATE TABLE IF NOT EXISTS pms_solution_like (
    id          BIGINT   NOT NULL,
    solution_id BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_solution_user (solution_id, user_id),
    KEY idx_user (user_id)
) ENGINE = InnoDB COMMENT '题解点赞关系表';
