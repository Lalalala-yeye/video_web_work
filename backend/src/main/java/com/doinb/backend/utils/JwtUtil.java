package com.doinb.backend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类：负责生成和解析登录令牌。
 * <p>
 * 登录成功后，服务端把用户 id、角色等信息写进 token；
 * 之后每次请求带上 token，服务端解析即可知道「是谁在访问」。
 */
@Component
public class JwtUtil {

    /** 从 application.yml / application-local.yml 读取密钥，至少 32 个字符 */
    @Value("${jwt.secret}")
    private String secret;

    /** token 有效期（毫秒），默认 86400000 = 24 小时 */
    @Value("${jwt.expire}")
    private long expireMillis;

    /**
     * 生成登录 token
     *
     * @param userId 用户 id
     * @param role   角色标识：user（普通/发布者）或 admin（管理员）
     */
    public String createToken(Integer userId, String role) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireMillis);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString().replace("-", "")) // 随机 id，让每次登录的 token 不同
                .setSubject(String.valueOf(userId))                    // 主体：用户 id
                .claim("role", role)                                   // 自定义字段：角色
                .setIssuedAt(now)
                .setExpiration(expireAt)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 token，取出里面的 Claims（声明信息）
     *
     * @return 解析成功返回 Claims；token 为空、过期或非法时返回 null
     */
    public Claims parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return null; // token 已过期
        } catch (Exception e) {
            return null; // token 格式错误或签名不对
        }
    }

    /** 从 token 里取出用户 id */
    public Integer getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null || !StringUtils.hasText(claims.getSubject())) {
            return null;
        }
        return Integer.valueOf(claims.getSubject());
    }

    /** 从 token 里取出角色（user / admin） */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return "";
        }
        Object role = claims.get("role");
        return role == null ? "" : role.toString();
    }

    /** 校验 token 是否有效（能解析且未过期） */
    public boolean isTokenValid(String token) {
        return parseToken(token) != null;
    }

    /** 用配置文件里的 secret 生成 HMAC 签名密钥 */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
