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

    public AuthDto.SignInResponse signin(AuthDto.SignInRequest request) {
        log.info("로그인 요청: {}", request.getLoginId());

        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 존재하지 않는 아이디: {}", request.getLoginId());
                    return new RuntimeException("아이디 또는 비밀번호가 잘못되었습니다.");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("로그인 실패 - 비밀번호 불일치");
            throw new RuntimeException("아이디 또는 비밀번호가 잘못되었습니다.");
        }

        String token = jwtProvider.generateToken(user.getLoginId(), user.getRoles());

        log.info("로그인 성공");

        return AuthDto.SignInResponse.from(user, token);
    }
}
