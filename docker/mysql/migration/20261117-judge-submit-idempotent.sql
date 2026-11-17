-- =============================================================
-- 判题幂等键调整为「用户级」：uk_submit_id(submit_id) -> uk_user_submit(user_id, submit_id)
-- 适用场景：已初始化过的库（docker/mysql/init 只在首次建卷时执行）
-- 全新部署无需执行，03-judge.sql 已包含新的唯一键
-- =============================================================
USE ai_oj_judge;

ALTER TABLE judge_submission
    DROP INDEX uk_submit_id,
    ADD UNIQUE KEY uk_user_submit (user_id, submit_id);
