package com.github.wuzhudexiaopacai.oj.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件记录实体（file_record）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_record")
public class FileRecord extends BaseEntity {

    /**
     * 文件 MD5（秒传与去重依据）
     */
    private String md5;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * MinIO 对象名（object key）
     */
    private String objectName;

    /**
     * 存储桶
     */
    private String bucket;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * MIME 类型
     */
    private String contentType;

    /**
     * 分片总数
     */
    private Integer chunkTotal;

    /**
     * 已上传分片数
     */
    private Integer chunkDone;

    /**
     * MinIO 分片上传会话ID（本系统采用"临时块 + composeObject 合并"方案，
     * MinIO 8.5 SDK 无公开 initiateMultipartUpload 接口，该字段固定存占位值 "compose"）
     */
    private String uploadId;

    /**
     * 业务类型：avatar/testcase/attachment/common
     */
    private String bizType;

    /**
     * 上传者
     */
    private Long uploaderId;

    /**
     * 状态：0上传中 1已完成 2已清理（超时未完成被延时任务清理）
     */
    private Integer status;
}
