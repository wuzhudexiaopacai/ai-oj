package com.github.wuzhudexiaopacai.oj.judge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 判题提交记录
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("judge_submission")
public class JudgeSubmission extends BaseEntity {

    /**
     * 幂等提交标识（客户端生成，唯一键防重）
     */
    private String submitId;

    /**
     * 提交用户
     */
    private Long userId;

    /**
     * 题目 ID
     */
    private Long problemId;

    /**
     * 比赛 ID，0 表示非比赛提交
     */
    private Long contestId;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 提交代码
     */
    private String code;

    /**
     * 代码长度
     */
    private Integer codeLength;

    /**
     * 判题状态（JudgeStatusEnum.code）
     */
    private Integer status;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 最大耗时（毫秒）
     */
    private Integer timeUsed;

    /**
     * 最大内存（KB）
     */
    private Integer memoryUsed;

    /**
     * 编译/运行错误信息
     */
    private String errorMessage;

    /**
     * 业务重试次数
     */
    private Integer retryCount;

    /**
     * 判题完成时间
     */
    private LocalDateTime judgeTime;

    /**
     * 乐观锁
     */
    @Version
    private Integer version;
}
