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

/**
 * JWT 생성 및 검증을 담당하는 컴포넌트입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final String KEY_ROLE = "roles";

    // JWT 유효 시간 (밀리초 단위) - application.properties에서 주입
    @Value("${spring.jwt.token-validity-in-ms}")
    private long tokenValidityInMs;

    // Base64 인코딩 된 시크릿 키 - application.properties에서 주입
    @Value("${spring.jwt.secret-key}")
    private String secretKeyStr;
  
    // JWT 서명 용 시크릿 키
    private SecretKey secretKey;

    /**
     * 시크릿 키 초기화 메서드입니다.
     * Base64로 인코딩된 시크릿 키를 디코딩하여 SecretKey로 변환합니다.
     * 애플리케이션 실행 시 한 번만 실행됩니다.
     */
    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKeyStr));
    }

    /**
     * 로그인 ID와 권한 목록을 기반으로 JWT 토큰을 생성합니다.
     *
     * @param loginId 사용자 로그인 ID
     * @param roles   사용자 권한 목록
     * @return 서명된 JWT 문자열
     */
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

    /**
     * JWT 에서 로그인 ID(subject)를 추출합니다.
     *
     * @param token JWT 문자열
     * @return 로그인 ID
     */
    public String getLoginId(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * JWT 유효성을 검사합니다.
     *
     * @param token 검증할 JWT 토큰
     * @return 유효하면 true, 아니면 false
     */
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


    /**
     * JWT 에서 파싱 및 Claims(payload)를 추출합니다.
     *
     * @param token JWT 문자열
     * @return Claims 객체
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}