package com.homework.web.admin.auth;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.web.admin.config.AdminJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/** 签发和解析仅供管理端使用的 JWT。 */
@Service
public class AdminJwtService {

    private static final String AUDIENCE = "homework-admin";

    private final AdminJwtProperties properties;
    private final SecretKey secretKey;
    private final JwtParser parser;

    public AdminJwtService(AdminJwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(properties.getSecretKey());
        } catch (DecodingException exception) {
            keyBytes = properties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_JWT_SECRET_KEY_TOO_SHORT);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.parser = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .requireIssuer(properties.getIssuer())
                .requireAudience(AUDIENCE)
                .build();
    }

    public String createToken(Long adminId, String email, String sessionKey, Integer sessionVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setIssuer(properties.getIssuer())
                .setAudience(AUDIENCE)
                .setSubject(adminId.toString())
                .claim("email", email)
                .claim("sid", sessionKey)
                .claim("ver", sessionVersion)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(properties.getTtlSeconds())))
                .signWith(secretKey)
                .compact();
    }

    public Claims parse(String token) {
        return parser.parseClaimsJws(token).getBody();
    }

    public long getTtlSeconds() {
        return properties.getTtlSeconds();
    }
}
