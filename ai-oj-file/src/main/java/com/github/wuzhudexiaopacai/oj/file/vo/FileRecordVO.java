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
@Schema(description = "文件记录视图（管理端分页返回）")
public class FileRecordVO implements Serializable {

    /**
     * 文件记录 ID
     */
    @Schema(description = "文件记录 ID（雪花 ID，JSON 序列化为字符串）", example = "1850000000000000001")
    private Long id;

    /**
     * 文件 MD5
     */
    @Schema(description = "文件 MD5（32 位十六进制）", example = "d41d8cd98f00b204e9800998ecf8427e")
    private String md5;

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名（含扩展名）", example = "solution.zip")
    private String fileName;

    /**
     * MinIO 对象名
     */
    @Schema(description = "MinIO 对象 key；上传中为空字符串，合并完成为 files/{md5}/{fileName}", example = "files/9e107d9d372bb6826bd81d3542a419d6/solution.zip")
    private String objectName;

    /**
     * 存储桶
     */
    @Schema(description = "MinIO 存储桶名称", example = "ai-oj-file")
    private String bucket;

    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件总大小（字节）", example = "10485760")
    private Long fileSize;

    /**
     * MIME 类型
     */
    @Schema(description = "MIME 类型；初始化时写入占位空字符串，可能为空", example = "application/zip")
    private String contentType;

    /**
     * 分片总数
     */
    @Schema(description = "分片总数", example = "2")
    private Integer chunkTotal;

    /**
     * 已上传分片数
     */
    @Schema(description = "已上传分片数；上传中为 0，合并成功后等于 chunkTotal", example = "2")
    private Integer chunkDone;

    /**
     * 业务类型
     */
    @Schema(description = "业务类型：avatar/testcase/attachment/common", example = "testcase")
    private String bizType;

    /**
     * 上传者
     */
    @Schema(description = "上传者用户 ID（雪花 ID，JSON 序列化为字符串）；无法识别登录用户时可能为 null", example = "1850000000000000002")
    private Long uploaderId;

    /**
     * 状态：0上传中 1已完成 2已清理
     */
    @Schema(description = "状态：0 上传中、1 已完成、2 已清理（超时未完成被延时任务清理）", example = "1")
    private Integer status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间（ISO-8601，如 2026-10-27T22:37:00）", example = "2026-10-27T22:37:00")
    private LocalDateTime createTime;
}
