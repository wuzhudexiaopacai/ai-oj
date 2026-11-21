package com.github.wuzhudexiaopacai.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题解点赞关系（pms_solution_like）
 * <p>
 * 不继承 BaseEntity：点赞取消需要物理删除，逻辑删除会继续占用唯一键导致无法重新点赞。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@TableName("pms_solution_like")
public class SolutionLike implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long solutionId;

    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
