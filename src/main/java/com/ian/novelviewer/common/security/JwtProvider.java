package com.ian.novelviewer.common.security;

import com.ian.novelviewer.common.enums.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final String KEY_ROLE = "roles";

    @Value("${spring.jwt.token-validity-in-ms}")
    private long tokenValidityInMs;

    @Value("${spring.jwt.secret-key}")
    private String secretKeyStr;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKeyStr));
    }

    public String generateToken(String loginId, List<Role> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + tokenValidityInMs);

        List<String> authorities = roles.stream()
                .map(role -> "ROLE_" + role.name())
                .toList();

        return Jwts.builder()
                .subject(loginId)
                .claim(KEY_ROLE, authorities)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getLoginId(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);

            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("JWT 서명 오류: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT 만료됨: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("잘못된 JWT: {}", e.getMessage());
        }

        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}