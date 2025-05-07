package com.ian.novelviewer.auth.application;

import com.ian.novelviewer.auth.dto.AuthDto;
import com.ian.novelviewer.common.security.JwtProvider;
import com.ian.novelviewer.user.domain.User;
import com.ian.novelviewer.user.domain.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * 회원가입을 처리합니다.
     *
     * @param request 회원가입 요청 DTO
     * @return AuthResponse (회원 정보 + 토큰)
     */
    @Transactional
    public AuthDto.SignUpResponse signup(AuthDto.SignUpRequest request) {
        log.info("회원가입 요청: {}", request.getLoginId());

        if (userRepository.existsByLoginId(request.getLoginId()))
            throw new RuntimeException("이미 사용 중인 아이디입니다.");

        if (userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("이미 가입된 이메일입니다.");

        User user = AuthDto.SignUpRequest.from(request);
        user.encodingPassword(passwordEncoder.encode(request.getPassword()));

        User result = userRepository.save(user);
        log.info("회원 저장 완료: {}", result.getLoginId());

        return AuthDto.SignUpResponse.from(result);
    }
}
