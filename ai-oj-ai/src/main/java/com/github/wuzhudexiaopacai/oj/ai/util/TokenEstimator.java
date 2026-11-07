package com.github.wuzhudexiaopacai.oj.ai.util;

/**
 * token 数估算工具
 *
 * <p>模型未返回 usage（如流式响应不带 token 统计）时用于兜底估算，
 * 规则：CJK 字符按 1 token/字，其余非空白字符按 4 字符/token 折算。</p>
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public final class TokenEstimator {

    /**
     * 非 CJK 字符折算 token 的分母
     */
    private static final int ASCII_CHARS_PER_TOKEN = 4;

    private TokenEstimator() {
    }

    /**
     * 估算文本消耗的 token 数
     *
     * @param text 文本（可为 null）
     * @return 估算 token 数，空文本为 0
     */
    public static int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int cjkChars = 0;
        int otherChars = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCjk(c)) {
                cjkChars++;
            } else if (!Character.isWhitespace(c)) {
                otherChars++;
            }
        }
        int tokens = cjkChars + (int) Math.ceil(otherChars / (double) ASCII_CHARS_PER_TOKEN);
        return Math.max(tokens, 1);
    }

    /**
     * 判断是否为 CJK 字符（汉字、CJK 标点、全角字符）
     */
    private static boolean isCjk(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF)
                || (c >= 0x3000 && c <= 0x303F)
                || (c >= 0xFF00 && c <= 0xFFEF);
    }
}
