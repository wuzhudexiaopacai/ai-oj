package com.github.wuzhudexiaopacai.oj.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分片上传初始化响应
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Builder
@Schema(description = "分片上传初始化响应（初始化与合并接口共用）")
public class UploadInitVO implements Serializable {

    /**
     * 文件记录 ID
     */
    @Schema(description = "文件记录 ID（雪花 ID，JSON 序列化为字符串）", example = "1850000000000000001")
    private Long fileId;

    /**
     * 是否秒传成功（MD5 命中已完成文件，无需再传分片）
     */
    @Schema(description = "是否已完成：true 表示秒传命中或合并成功、无需再传分片；false 表示仍需上传分片", example = "false")
    private Boolean uploaded;

    /**
     * 已上传分片序号列表（断点续传：客户端跳过这些分片）
     */
    @Schema(description = "已上传分片序号（从 1 开始、升序），断点续传时客户端跳过这些分片；全新上传或秒传时为空列表", example = "[1,2]")
    private List<Integer> uploadedChunks;

    /**
     * 约定的分片大小（字节）
     */
    @Schema(description = "服务端约定的分片大小（字节），默认 5242880（5MB）；客户端按此大小切片，最后一片除外", example = "5242880")
    private Long chunkSize;
}
