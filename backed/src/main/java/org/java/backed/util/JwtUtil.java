package org.java.backed.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    // 生产环境应放配置文件
    private static final String SECRET = "SchoolDormitorySystemJwtSecretKey2024!@#$%^";
    private static final long EXPIRE_MS = 24 * 60 * 60 * 1000; // 24小时

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String role, List<String> permissions) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("permissions", permissions)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE_MS))
                .signWith(getKey())
                .compact();
    }

    @SuppressWarnings("unchecked")
    public List<String> getPermissions(String token) {
        Claims claims = parseToken(token);
        Object obj = claims.get("permissions");
        return obj instanceof List<?> ? ((List<?>) obj).stream().map(Object::toString).toList() : List.of();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public boolean isExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public boolean validate(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
