package com.homework.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {
    private final String issuer;
    private final long ttlSeconds; //有效期
    private final SecretKey secretKey; //签名密钥
    private final JwtParser jwtParser; //JWT 解析器

    public JwtUtil(@Value("${jwt.issuer}") String issuer,
                   @Value("${jwt.secret-key}") String secretKey,
                   @Value("${jwt.ttl-seconds}") long ttlSeconds) {
        validateConfig(issuer, secretKey, ttlSeconds);
        this.issuer = issuer;
        this.secretKey = createSecretKey(secretKey);
        this.ttlSeconds = ttlSeconds;
        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(this.secretKey)
                .requireIssuer(issuer)
                .build();
    }

    private void validateConfig(String issuer, String secretKey, long ttlSeconds) {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("JWT configuration error: jwt.issuer must not be blank");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("JWT configuration error: jwt.secret-key is missing. Set JWT_SECRET_KEY or configure jwt.secret-key");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalStateException("JWT configuration error: jwt.ttl-seconds must be greater than 0");
        }
    }

    private SecretKey createSecretKey(String secretKey) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey); //支持 Base64 编码后的密钥。
        } catch (DecodingException e) {
            keyBytes = secretKey.getBytes(StandardCharsets.UTF_8); //也支持普通字符串密钥，便于本地和环境变量配置。
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT configuration error: jwt.secret-key must be at least 32 bytes after decoding");
        }
        return Keys.hmacShaKeyFor(keyBytes); //把字节数组转换成 JWT 签名可以使用的密钥对象。
    }


    //生成token的方法：登陆成功后，根据 用户Id 和 用户名 生成JWT
    public String createToken(Long userId, String accountNo) {

        Instant now = Instant.now(); //获取当前时间
        Instant expiration = now.plusSeconds(ttlSeconds); //计算 token 过期时间


        //createToken方法中 真正创建JWT的地方
        return Jwts.builder()
                .setIssuer(issuer) //设置签发者
                .setSubject(userId.toString()) //设置主题，含义：这个 token 属于 userId = 1001 的用户（id 比 username 稳定，可以把subject 理解成 id）。
                .claim("accountNo", accountNo) //添加自定义信息：这里就是在前端的payload里，增加username字段
                .setIssuedAt(Date.from(now)) //设置签发时间
                .setExpiration(Date.from(expiration)) //设置过期时间
                .signWith(secretKey)
                /*
                用密钥签名，这是JWT安全性的核心。
                签名的作用是：防止 token 被篡改。
                 */

                .compact(); //把前面设置的内容压缩生成最终的 JWT 字符串。
    }

    //解析 JWT，并返回里面的用户信息 给后端开发自己看的
    public Claims parseToken(String token) {
        return jwtParser
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserId(String token) {
        String subject = parseToken(token).getSubject();
        return Long.valueOf(subject);
    }

    public String getAccountNo(String token) {
        return parseToken(token).get("accountNo", String.class);
    }
}
