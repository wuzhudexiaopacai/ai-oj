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
@Schema(description = "分片上传初始化请求：提交文件元信息，服务端据此判断秒传/续传并返回上传上下文")
public class UploadInitRequest implements Serializable {

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名（含扩展名），合并后作为对象 key 的末段", example = "solution.zip",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件总大小（字节），写入 file_record.file_size", example = "10485760",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文件大小不能为空")
    private Long size;

    /**
     * 文件 MD5（秒传与断点续传依据）
     */
    @Schema(description = "文件 MD5（32 位十六进制小写），秒传与断点续传的唯一依据", example = "d41d8cd98f00b204e9800998ecf8427e",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文件 MD5 不能为空")
    private String md5;

    /**
     * 分片总数
     */
    @Schema(description = "分片总数，至少为 1；分片大小由服务端约定（默认 5MB）", example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分片总数不能为空")
    @Min(value = 1, message = "分片总数至少为 1")
    private Integer chunkTotal;

    /**
     * 业务类型：avatar/testcase/attachment/common
     */
    @Schema(description = "业务类型：avatar/testcase/attachment/common，为空时默认 common", example = "testcase")
    private String bizType;
}
