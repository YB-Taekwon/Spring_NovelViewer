package com.ian.novelviewer.auth.ui;

import com.ian.novelviewer.auth.application.AuthService;
import com.ian.novelviewer.auth.dto.AuthDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입 API
     *
     * @param request 회원가입 요청 DTO
     * @return 회원정보 + JWT 토큰
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid AuthDto.SignUp request) {
        log.info("회원가입 요청 수신: {}", request.getLoginId());
        AuthDto.AuthResponse response = authService.signup(request);

        return ResponseEntity.ok(response);
    }

    /**
     * 로그인 API
     *
     * @param request 로그인 요청 DTO
     * @return 회원정보 + JWT 토큰
     */
    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody @Valid AuthDto.SignIn request) {
        log.info("로그인 요청 수신: {}", request.getLoginId());
        AuthDto.AuthResponse response = authService.signin(request);

        return ResponseEntity.ok(response);
    }
}
