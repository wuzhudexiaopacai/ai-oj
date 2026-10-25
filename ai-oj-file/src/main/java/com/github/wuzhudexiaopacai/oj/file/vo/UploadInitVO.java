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
@Schema(description = "分片上传初始化响应")
public class UploadInitVO implements Serializable {

    /**
     * 文件记录 ID
     */
    @Schema(description = "文件记录 ID")
    private Long fileId;

    /**
     * 是否秒传成功（MD5 命中已完成文件，无需再传分片）
     */
    @Schema(description = "是否秒传成功")
    private Boolean uploaded;

    /**
     * 已上传分片序号列表（断点续传：客户端跳过这些分片）
     */
    @Schema(description = "已上传分片序号列表")
    private List<Integer> uploadedChunks;

    /**
     * 约定的分片大小（字节）
     */
    @Schema(description = "分片大小（字节）")
    private Long chunkSize;
}
