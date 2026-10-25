-- =============================================================
-- AI-OJ 文件库（MinIO 大文件分片上传元数据）
-- @author wuzhudexiaopacai
-- =============================================================
CREATE DATABASE IF NOT EXISTS ai_oj_file DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_oj_file;

SET NAMES utf8mb4;

-- -----------------------------
-- 文件记录（支持秒传 / 分片断点续传）
-- -----------------------------
DROP TABLE IF EXISTS file_record;
CREATE TABLE file_record (
    id           BIGINT       NOT NULL,
    md5          VARCHAR(64)  NOT NULL COMMENT '文件 MD5（秒传与去重依据）',
    file_name    VARCHAR(255) NOT NULL COMMENT '原始文件名',
    object_name  VARCHAR(512) NOT NULL DEFAULT '' COMMENT 'MinIO 对象名',
    bucket       VARCHAR(64)  NOT NULL DEFAULT 'ai-oj' COMMENT '存储桶',
    file_size    BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    content_type VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'MIME 类型',
    chunk_total  INT          NOT NULL DEFAULT 1 COMMENT '分片总数',
    chunk_done   INT          NOT NULL DEFAULT 0 COMMENT '已上传分片数',
    upload_id    VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'MinIO 分片上传会话ID',
    biz_type     VARCHAR(32)  NOT NULL DEFAULT 'common' COMMENT '业务类型：avatar/testcase/attachment',
    uploader_id  BIGINT       NULL COMMENT '上传者',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0上传中 1已完成 2已失败（见 FileStatusEnum）',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by    BIGINT       NULL,
    update_by    BIGINT       NULL,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md5 (md5),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT '文件记录表';
