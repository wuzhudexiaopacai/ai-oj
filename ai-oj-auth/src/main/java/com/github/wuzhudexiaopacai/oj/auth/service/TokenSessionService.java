package com.github.wuzhudexiaopacai.oj.auth.service;

import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户令牌会话管理
 * <p>
 * 维护 userId -> 已签发 jti 集合，配合注销黑名单，实现：
 * 登出/刷新时单个令牌失效，禁用或删除账号时该用户全部令牌立即失效。
 * 索引 key 与黑名单使用同一有效期，过期后令牌本身也已失效，无需额外清理。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class TokenSessionService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 令牌有效期（分钟），索引与黑名单保留时长与之对齐
     */
    @Value("${ai-oj.jwt.expire-minutes:720}")
    private long expireMinutes;

    /**
     * 记录新签发的令牌
     *
     * @param userId  用户 ID
     * @param tokenId 令牌 jti
     */
    public void register(Long userId, String tokenId) {
        String key = RedisKeys.USER_TOKENS + userId;
        stringRedisTemplate.opsForSet().add(key, tokenId);
        stringRedisTemplate.expire(key, expireMinutes, TimeUnit.MINUTES);
    }

    /**
     * 使单个令牌失效（登出/刷新旧令牌）
     *
     * @param userId  用户 ID
     * @param tokenId 令牌 jti
     */
    public void revoke(Long userId, String tokenId) {
        stringRedisTemplate.opsForSet().remove(RedisKeys.USER_TOKENS + userId, tokenId);
        stringRedisTemplate.opsForValue().set(RedisKeys.TOKEN_BLACKLIST + tokenId, "1",
                expireMinutes, TimeUnit.MINUTES);
    }

    /**
     * 使该用户全部已签发令牌失效（禁用/删除账号）
     *
     * @param userId 用户 ID
     */
    public void revokeAll(Long userId) {
        String key = RedisKeys.USER_TOKENS + userId;
        Set<String> tokenIds = stringRedisTemplate.opsForSet().members(key);
        if (tokenIds != null) {
            for (String tokenId : tokenIds) {
                stringRedisTemplate.opsForValue().set(RedisKeys.TOKEN_BLACKLIST + tokenId, "1",
                        expireMinutes, TimeUnit.MINUTES);
            }
        }
        stringRedisTemplate.delete(key);
    }
}
