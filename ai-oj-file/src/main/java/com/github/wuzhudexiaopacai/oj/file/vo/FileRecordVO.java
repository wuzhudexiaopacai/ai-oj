package com.github.wuzhudexiaopacai.oj.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件记录视图
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "文件记录视图")
public class FileRecordVO implements Serializable {

    /**
     * 文件记录 ID
     */
    @Schema(description = "文件记录 ID")
    private Long id;

    /**
     * 文件 MD5
     */
    @Schema(description = "文件 MD5")
    private String md5;

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名")
    private String fileName;

    /**
     * MinIO 对象名
     */
    @Schema(description = "MinIO 对象名")
    private String objectName;

    /**
     * 存储桶
     */
    @Schema(description = "存储桶")
    private String bucket;

    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    /**
     * MIME 类型
     */
    @Schema(description = "MIME 类型")
    private String contentType;

    /**
     * 分片总数
     */
    @Schema(description = "分片总数")
    private Integer chunkTotal;

    /**
     * 已上传分片数
     */
    @Schema(description = "已上传分片数")
    private Integer chunkDone;

    /**
     * 业务类型
     */
    @Schema(description = "业务类型")
    private String bizType;

    /**
     * 上传者
     */
    @Schema(description = "上传者")
    private Long uploaderId;

    /**
     * 状态：0上传中 1已完成 2已清理
     */
    @Schema(description = "状态：0上传中 1已完成 2已清理")
    private Integer status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
