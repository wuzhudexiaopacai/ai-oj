package com.github.wuzhudexiaopacai.oj.ai.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TokenEstimator 单元测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
class TokenEstimatorTest {

    @Test
    void nullOrEmptyReturnsZero() {
        assertEquals(0, TokenEstimator.estimate(null));
        assertEquals(0, TokenEstimator.estimate(""));
    }

    @Test
    void cjkCharsCountAsOneTokenEach() {
        // 4 个汉字 -> 4 tokens
        assertEquals(4, TokenEstimator.estimate("你好世界"));
    }

    @Test
    void asciiCharsCountFourPerToken() {
        // 8 个字母 -> 2 tokens
        assertEquals(2, TokenEstimator.estimate("abcdefgh"));
        // 5 个字母 -> 向上取整 2 tokens
        assertEquals(2, TokenEstimator.estimate("abcde"));
    }

    @Test
    void whitespaceIgnored() {
        // 空白不计入字符数，"a b c d" 只有 4 个非空白字符 -> 1 token
        assertEquals(1, TokenEstimator.estimate("a b c d"));
    }

    @Test
    void mixedTextCombinesBothRules() {
        // 2 个汉字 + 4 个字母 = 2 + 1 = 3 tokens
        assertEquals(3, TokenEstimator.estimate("中文code"));
    }

    @Test
    void nonBlankTextNeverReturnsZero() {
        assertEquals(1, TokenEstimator.estimate("a"));
    }
}
