package com.ian.novelviewer.auth.application;

import com.ian.novelviewer.auth.dto.AuthDto;
import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.security.JwtProvider;
import com.ian.novelviewer.user.domain.User;
import com.ian.novelviewer.user.domain.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.ian.novelviewer.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;


    /**
     * 회원 가입을 처리합니다.
     *
     * @param request 회원 가입 요청 정보 (로그인 ID, 이메일, 비밀번호 등)
     * @return 회원 가입 응답 DTO (가입된 사용자 정보)
     * @throws CustomException 로그인 ID 또는 이메일이 중복된 경우
     */
    @Transactional
    public AuthDto.SignUpResponse signup(AuthDto.SignUpRequest request) {
        log.debug("회원 가입 시도 - loginId: {}", request.getLoginId());

        if (userRepository.existsByLoginId(request.getLoginId())) {
            log.error("중복된 로그인 ID: {}", request.getLoginId());
            throw new CustomException(DUPLICATE_LOGIN_ID);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.error("중복된 이메일: {}", request.getEmail());
            throw new CustomException(DUPLICATE_EMAIL);
        }

        User user = AuthDto.SignUpRequest.from(request);
        user.encodingPassword(passwordEncoder.encode(request.getPassword()));

        User result = userRepository.save(user);

        log.debug("회원 가입 성공 - loginId: {}", result.getLoginId());
        return AuthDto.SignUpResponse.from(result);
    }


    /**
     * 사용자 로그인을 처리합니다.
     *
     * @param request 로그인 요청 정보 (로그인 ID, 비밀번호)
     * @return 로그인 응답 DTO (사용자 정보 및 JWT 토큰)
     * @throws CustomException 로그인 정보가 유효하지 않은 경우
     */
    public AuthDto.SignInResponse signin(AuthDto.SignInRquest request) {
        log.debug("로그인 시도 - loginId: {}", request.getLoginId());

        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> {
                    log.error("로그인 실패 - 존재하지 않는 loginId: {}", request.getLoginId());
                    return new CustomException(INVALID_CREDENTIALS);
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.error("로그인 실패 - 비밀번호 불일치: {}", request.getLoginId());
            throw new CustomException(INVALID_CREDENTIALS);
        }

        String token = jwtProvider.generateToken(user.getLoginId(), user.getRoles());

        log.debug("로그인 성공 - loginId: {}", request.getLoginId());
        return AuthDto.SignInResponse.from(user, token);
    }


    /**
     * 로그아웃 처리를 수행합니다.
     * JWT 토큰을 Redis에 저장하여 블랙리스트로 등록합니다.
     *
     * @param token 로그아웃할 사용자의 JWT 토큰
     */
    public void signout(String token) {
        log.debug("로그아웃 처리 시작 - 토큰: {}", token != null ? token.substring(0, Math.min(token.length(), 15))
                + "..." : "null");

        long expiration = jwtProvider.getExpiration(token);
        log.debug("토큰 남은 유효시간(ms): {}", expiration);

        redisTemplate.opsForValue().set(token, "signout", expiration, TimeUnit.MILLISECONDS);

        log.debug("Redis에 로그아웃 토큰 저장 완료 - key: {}, TTL(ms): {}", token, expiration);
    }
}