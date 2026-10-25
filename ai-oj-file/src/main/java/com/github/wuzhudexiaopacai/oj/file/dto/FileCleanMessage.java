package com.github.wuzhudexiaopacai.oj.file.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文件清理延时消息（分片超时未完成时兜底清理临时块）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileCleanMessage implements Serializable {

    /**
     * 文件记录 ID
     */
    private Long fileId;

    /**
     * 文件 MD5
     */
    private String md5;
}
