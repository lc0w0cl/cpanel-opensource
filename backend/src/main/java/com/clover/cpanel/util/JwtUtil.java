package com.clover.cpanel.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtil {

    /**
     * JWT密钥
     */
    @Value("${jwt.secret:cpanel-jwt-secret-key-for-authentication-system-2024}")
    private String secret;

    /**
     * JWT过期时间（毫秒）- 默认24小时
     */
    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    /**
     * 刷新token过期时间（毫秒）- 默认7天
     */
    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshExpiration;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成访问token
     * @param subject 主题（通常是用户ID或用户名）
     * @return JWT token
     */
    public String generateAccessToken(String subject) {
        return generateToken(subject, expiration, "access");
    }

    /**
     * 生成刷新token
     * @param subject 主题（通常是用户ID或用户名）
     * @return JWT refresh token
     */
    public String generateRefreshToken(String subject) {
        return generateToken(subject, refreshExpiration, "refresh");
    }

    /**
     * 生成token
     * @param subject 主题
     * @param expiration 过期时间
     * @param tokenType token类型
     * @return JWT token
     */
    private String generateToken(String subject, Long expiration, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", tokenType);
        claims.put("iat", now.getTime() / 1000);

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从token中获取主题
     * @param token JWT token
     * @return 主题
     */
    public String getSubjectFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("获取token主题失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从token中获取Claims
     * @param token JWT token
     * @return Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证token是否有效
     * @param token JWT token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !isTokenExpired(claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证访问token
     * @param token JWT token
     * @return 是否有效
     */
    public boolean validateAccessToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String tokenType = (String) claims.get("type");
            return "access".equals(tokenType) && !isTokenExpired(claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("访问Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证刷新token
     * @param token JWT token
     * @return 是否有效
     */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String tokenType = (String) claims.get("type");
            return "refresh".equals(tokenType) && !isTokenExpired(claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("刷新Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查token是否过期
     * @param claims Claims
     * @return 是否过期
     */
    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }

    /**
     * 获取token过期时间
     * @param token JWT token
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("获取token过期时间失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取token剩余有效时间（秒）
     * @param token JWT token
     * @return 剩余有效时间（秒）
     */
    public long getTokenRemainingTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            if (expiration == null) {
                return 0;
            }
            long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
        } catch (Exception e) {
            log.error("获取token剩余时间失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 从HTTP请求头中提取token
     * @param authHeader Authorization头
     * @return JWT token
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
