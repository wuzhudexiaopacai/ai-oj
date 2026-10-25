package com.github.wuzhudexiaopacai.oj.common.core.util;

import cn.hutool.core.util.StrUtil;
import com.github.wuzhudexiaopacai.oj.common.core.constant.CommonConstants;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 工具（HS256，密钥通过 Nacos 配置注入）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public class JwtUtils {

    private final SecretKey secretKey;

    private final long expireMillis;

    public JwtUtils(String base64Secret, long expireMillis) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expireMillis = expireMillis;
    }

    /**
     * 生成 HS256 可用的 Base64 密钥（运维生成后配置到 Nacos）
     *
     * @return Base64 密钥
     */
    public static String generateSecret() {
        return Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
    }

    /**
     * 签发 Token
     *
     * @param loginUser 登录用户
     * @return JWT 字符串
     */
    public String generateToken(LoginUser loginUser) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        loginUser.setTokenId(jti);
        Map<String, Object> claims = new HashMap<>(8);
        claims.put(CommonConstants.JWT_CLAIM_UID, loginUser.getId());
        claims.put(CommonConstants.JWT_CLAIM_NICKNAME, loginUser.getNickname());
        claims.put(CommonConstants.JWT_CLAIM_AUTHORITIES, StrUtil.join(",", loginUser.getAuthorities()));
        Date now = new Date();
        return Jwts.builder()
                .id(jti)
                .subject(loginUser.getUsername())
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 Token（验签失败/过期会抛出 JwtException，调用方捕获处理）
     *
     * @param token JWT 字符串
     * @return 登录用户
     */
    public LoginUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String auths = claims.get(CommonConstants.JWT_CLAIM_AUTHORITIES, String.class);
        List<String> authorityList = StrUtil.isBlank(auths) ? List.of() : Arrays.asList(auths.split(","));
        return LoginUser.builder()
                .id(claims.get(CommonConstants.JWT_CLAIM_UID, Long.class))
                .username(claims.getSubject())
                .nickname(claims.get(CommonConstants.JWT_CLAIM_NICKNAME, String.class))
                .authorities(authorityList)
                .tokenId(claims.getId())
                .build();
    }
}
