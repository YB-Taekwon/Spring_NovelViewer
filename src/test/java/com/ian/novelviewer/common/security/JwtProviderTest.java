package com.ian.novelviewer.common.security;

import com.ian.novelviewer.common.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static com.ian.novelviewer.common.enums.Role.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtProvider 클래스의 단위 테스트입니다.
 * - 토큰 생성 및 유효성 검증
 * - 만료 토큰 처리
 */
class JwtProviderTest {
    private static final Logger log = LoggerFactory.getLogger(JwtProviderTest.class);

    private static final String LOGIN_ID = "testid";
    private static final List<Role> ROLES = List.of(ROLE_USER);

    private JwtProvider jwtProvider;

    /**
     * 테스트 실행 전 JWT Provider 초기 설정을 수행합니다.
     * - Secret 키를 Base64로 인코딩하여 주입
     * - 유효 시간 설정
     */
    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();

        String rawSecret = "spring-security-test-jwt-secret-key";
        String base64Secret = Base64.getEncoder().encodeToString(rawSecret.getBytes(StandardCharsets.UTF_8));

        ReflectionTestUtils.setField(jwtProvider, "tokenValidityInMs", 1000 * 60 * 60);
        ReflectionTestUtils.setField(jwtProvider, "secretKeyStr", base64Secret);
        jwtProvider.init();
    }

    /**
     * 유효한 JWT 토큰을 생성한 후, 해당 토큰이 정상적으로 검증되고
     * 사용자 ID를 정확히 추출할 수 있는지 테스트합니다.
     *
     * @see JwtProvider#generateToken(String, List)
     * @see JwtProvider#validateToken(String)
     * @see JwtProvider#getLoginId(String)
     */
    @Test
    @DisplayName("유효한 토큰은 검증에 성공해야 한다.")
    void generateAndValidateToken() {
        // given

        // when
        String token = jwtProvider.generateToken(LOGIN_ID, ROLES);
        boolean isValid = jwtProvider.validateToken(token);
        String extractedLoginId = jwtProvider.getLoginId(token);

        // then
        log.info("Generated JWT: {}", token);
        log.info("Extracted LoginId from Token: {}", extractedLoginId);

        assertThat(isValid).isTrue();
        assertThat(extractedLoginId).isEqualTo(LOGIN_ID);
    }

    /**
     * 매우 짧은 유효 기간의 토큰을 생성한 후,
     * 만료 시간이 지난 후 검증을 수행하여 실패하는지 테스트합니다.
     *
     * @throws InterruptedException Thread.sleep 중 발생 가능
     * @see JwtProvider#generateToken(String, List)
     * @see JwtProvider#validateToken(String)
     */
    @Test
    @DisplayName("만료된 토큰은 검증에서 실패해야 한다.")
    void expiredTokenShouldBeInvalid() throws InterruptedException {
        // given
        ReflectionTestUtils.setField(jwtProvider, "tokenValidityInMs", 1);
        jwtProvider.init();

        String token = jwtProvider.generateToken(LOGIN_ID, ROLES);

        Thread.sleep(10);

        // when
        boolean isValid = jwtProvider.validateToken(token);

        // then
        log.info("Expired Token: {}", token);
        assertThat(isValid).isFalse();
    }
}