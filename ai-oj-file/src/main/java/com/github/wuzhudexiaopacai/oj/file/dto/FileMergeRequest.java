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
@Schema(description = "分片合并请求")
public class FileMergeRequest implements Serializable {

    /**
     * 文件 MD5
     */
    @Schema(description = "文件 MD5")
    @NotBlank(message = "文件 MD5 不能为空")
    private String md5;
}
