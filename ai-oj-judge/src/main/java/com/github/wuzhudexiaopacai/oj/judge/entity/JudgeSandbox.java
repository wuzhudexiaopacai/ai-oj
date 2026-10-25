package com.github.wuzhudexiaopacai.oj.judge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 评测沙箱实例
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("judge_sandbox")
public class JudgeSandbox extends BaseEntity {

    /**
     * 沙箱唯一标识
     */
    private String sandboxKey;

    /**
     * 沙箱 HTTP 地址
     */
    private String baseUrl;

    /**
     * 最大并行判题数
     */
    private Integer maxParallel;

    /**
     * 状态：1 在线 0 离线
     */
    private Integer status;

    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeat;
}
