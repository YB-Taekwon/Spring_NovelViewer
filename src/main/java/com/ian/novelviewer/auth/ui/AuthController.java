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

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid AuthDto.SignUpRequest request) {
        AuthDto.SignUpResponse response = authService.signup(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody @Valid AuthDto.SignInRquest request) {
        log.info("로그인 요청 수신: {}", request.getLoginId());
        AuthDto.SignInResponse response = authService.signin(request);

        return ResponseEntity.ok(response);
    }
}