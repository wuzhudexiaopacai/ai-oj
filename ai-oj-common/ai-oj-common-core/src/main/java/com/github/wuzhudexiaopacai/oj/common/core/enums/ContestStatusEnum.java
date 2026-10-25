package com.github.wuzhudexiaopacai.oj.common.core.enums;

import lombok.Getter;

/**
 * 比赛状态机状态枚举
 * <p>
 * 状态流转：SCHEDULED → RUNNING → ENDED；SCHEDULED/RUNNING → CANCELED
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Getter
public enum ContestStatusEnum {

    /**
     * 未开始
     */
    SCHEDULED(0, "未开始"),

    /**
     * 进行中
     */
    RUNNING(1, "进行中"),

    /**
     * 已结束
     */
    ENDED(2, "已结束"),

    /**
     * 已取消
     */
    CANCELED(3, "已取消");

    private final int code;

    private final String desc;

    ContestStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ContestStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ContestStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
