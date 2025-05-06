package com.ian.novelviewer.auth.application;

import com.ian.novelviewer.auth.dto.AuthDto;
import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.exception.ErrorCode;
import com.ian.novelviewer.common.security.JwtProvider;
import com.ian.novelviewer.user.domain.User;
import com.ian.novelviewer.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.ian.novelviewer.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * 회원가입을 처리하고 자동 로그인 방식으로 토큰을 포함한 응답을 반환합니다.
     *
     * @param request 회원가입 요청 DTO
     * @return 회원 정보 + JWT 토큰
     */
    public AuthDto.AuthResponse signup(AuthDto.SignUp request) {
        log.info("회원가입 요청: {}", request.getLoginId());

        if (userRepository.existsByLoginId(request.getLoginId())) {
            log.warn("중복된 로그인 아이디: {}", request.getLoginId());
            throw new CustomException(DUPLICATE_LOGIN_ID);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("중복된 이메일: {}", request.getEmail());
            throw new CustomException(DUPLICATE_EMAIL);
        }

        request.setPassword(passwordEncoder.encode(request.getPassword()));

        User user = AuthDto.SignUp.from(request);
        User result = userRepository.save(user);

        log.info("회원 저장 완료: {}", result.getLoginId());

        String token = jwtProvider.generateToken(result.getLoginId(), result.getRoles());

        log.info("토큰 발급 완료");

        return AuthDto.AuthResponse.from(result, token);
    }

    /**
     * 로그인 요청을 처리하고 인증 성공 시 JWT 토큰과 사용자 정보를 반환합니다.
     *
     * @param request 로그인 요청 DTO
     * @return 회원 정보 + JWT 토큰
     */
    public AuthDto.AuthResponse signin(AuthDto.SignIn request) {
        log.info("로그인 요청: {}", request.getLoginId());

        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 존재하지 않는 아이디: {}", request.getLoginId());
                    return new CustomException(INVALID_CREDENTIALS);
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("로그인 실패 - 비밀번호 불일치");
            throw new CustomException(INVALID_CREDENTIALS);
        }

        String token = jwtProvider.generateToken(user.getLoginId(), user.getRoles());

        log.info("로그인 성공");

        return AuthDto.AuthResponse.from(user, token);
    }
}
