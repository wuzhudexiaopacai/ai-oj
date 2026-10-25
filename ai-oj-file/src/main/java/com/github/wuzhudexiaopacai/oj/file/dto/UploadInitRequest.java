package com.github.wuzhudexiaopacai.oj.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 分片上传初始化请求
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "分片上传初始化请求")
public class UploadInitRequest implements Serializable {

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名")
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件大小（字节）")
    @NotNull(message = "文件大小不能为空")
    private Long size;

    /**
     * 文件 MD5（秒传与断点续传依据）
     */
    @Schema(description = "文件 MD5")
    @NotBlank(message = "文件 MD5 不能为空")
    private String md5;

    /**
     * 分片总数
     */
    @Schema(description = "分片总数")
    @NotNull(message = "分片总数不能为空")
    @Min(value = 1, message = "分片总数至少为 1")
    private Integer chunkTotal;

    /**
     * 业务类型：avatar/testcase/attachment/common
     */
    @Schema(description = "业务类型")
    private String bizType;
}
