package com.github.wuzhudexiaopacai.oj.common.core.enums;

import lombok.Getter;

/**
 * 分片上传文件状态枚举
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Getter
public enum FileStatusEnum {

    /**
     * 已初始化（已申请上传，等待分片）
     */
    INIT,

    /**
     * 上传中
     */
    UPLOADING,

    /**
     * 已完成（分片合并成功）
     */
    COMPLETED,

    /**
     * 已中止
     */
    ABORTED
}
