package com.github.wuzhudexiaopacai.oj.common.core.result;

import lombok.Getter;

/**
 * 统一返回码
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Getter
public enum ResultCode {

    /**
     * 通用码段
     */
    SUCCESS(200, "操作成功"),
    FAILED(500, "系统繁忙，请稍后重试"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限执行该操作"),
    NOT_FOUND(404, "请求的资源不存在"),
    CONFLICT(409, "请勿重复提交"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    /**
     * 判题相关码段 6xx
     */
    JUDGE_BUSY(601, "判题机繁忙，请稍后重试"),
    SANDBOX_UNAVAILABLE(602, "评测沙箱暂不可用"),

    /**
     * 比赛相关码段 7xx
     */
    CONTEST_NOT_START(701, "比赛尚未开始"),
    CONTEST_ENDED(702, "比赛已结束"),
    CONTEST_NEED_REGISTER(703, "请先报名比赛");

    private final int code;

    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
