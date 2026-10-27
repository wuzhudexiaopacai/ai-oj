package com.github.wuzhudexiaopacai.oj.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 分片合并请求
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "分片合并请求：按文件 MD5 找到上传会话，校验分片齐全后服务端合并")
public class FileMergeRequest implements Serializable {

    /**
     * 文件 MD5
     */
    @Schema(description = "文件 MD5（32 位十六进制小写），须与初始化时一致", example = "d41d8cd98f00b204e9800998ecf8427e",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文件 MD5 不能为空")
    private String md5;
}
