package com.ian.novelviewer.auth.ui;

import com.ian.novelviewer.auth.application.AuthService;
import com.ian.novelviewer.auth.dto.AuthDto;
import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.exception.ErrorCode;
import com.ian.novelviewer.common.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ian.novelviewer.common.exception.ErrorCode.INVALID_TOKEN;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;


    /**
     * 회원가입 요청을 처리합니다.
     *
     * @param request 회원가입 요청 DTO
     * @return 회원가입 결과 응답 DTO
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid AuthDto.SignUpRequest request) {
        log.info("회원가입 요청 - loginId: {}", request.getLoginId());

        AuthDto.SignUpResponse response = authService.signup(request);

        log.info("회원가입 성공 - loginId: {}", response.getLoginId());
        return ResponseEntity.ok(response);
    }


    /**
     * 로그인 요청을 처리합니다.
     *
     * @param request 로그인 요청 DTO
     * @return 로그인 결과 응답 DTO (토큰 포함)
     */
    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody @Valid AuthDto.SignInRquest request) {
        log.info("로그인 요청 - loginId: {}", request.getLoginId());

        AuthDto.SignInResponse response = authService.signin(request);

        log.info("로그인 성공 - loginId: {}", response.getLoginId());
        return ResponseEntity.ok(response);
    }


    /**
     * 로그아웃 요청을 처리합니다. JWT 토큰을 블랙리스트(예: Redis)에 등록합니다.
     *
     * @param request HTTP 요청 객체 (Authorization 헤더에서 토큰 추출)
     * @return 로그아웃 성공 메시지
     */
    @PostMapping("/signout")
    public ResponseEntity<?> signout(HttpServletRequest request) {
        String token = jwtProvider.resolveToken(request);
        log.info("로그아웃 요청 - 토큰: {}", token != null ? token.substring(0, Math.min(token.length(), 15))
                + "..." : "null");

        authService.signout(token);

        log.info("로그아웃 성공 - 토큰 처리 완료");
        return ResponseEntity.ok("정상적으로 로그아웃이 완료되었습니다.");
    }
}