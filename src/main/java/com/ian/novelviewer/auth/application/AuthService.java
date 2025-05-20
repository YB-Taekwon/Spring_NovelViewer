package com.ian.novelviewer.auth.application;

import com.ian.novelviewer.auth.dto.AuthDto;
import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.redis.RedisKeyUtil;
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
    private final MailgunService mailgunService;
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

        String email = request.getEmail();
        String key = RedisKeyUtil.emailVerifyKey(email);
        String verified = redisTemplate.opsForValue().get(key);

        if (!"true".equals(verified)) {
            log.error("이메일 인증 안됨: {}", email);
            throw new CustomException(EMAIL_NOT_VERIFIED);
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


    /**
     * 주어진 이메일 주소로 인증 코드를 생성하여 전송합니다.
     * 인증 코드는 Redis에 5분간 저장되며, Mailgun을 통해 이메일로 발송됩니다.
     *
     * @param email 인증 코드를 받을 사용자 이메일 주소
     */
    public void sendVerificationCode(String email) {
        String code = generateCode();
        String key = RedisKeyUtil.emailVerifyKey(email);

        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
        log.debug("인증 코드 5분 간 Redis에 저장 - 이메일: {}, 인증 코드: {}", email, code);

        mailgunService.sendEmail(email, code);
    }


    /**
     * 사용자가 입력한 인증 코드가 유효한지 검증합니다.
     * 올바른 코드일 경우 Redis에 인증 완료 상태("true")를 10분간 저장합니다.
     *
     * @param email     인증을 시도하는 사용자 이메일 주소
     * @param inputCode 사용자가 입력한 인증 코드
     */
    public void verifyCode(String email, String inputCode) {
        String key = RedisKeyUtil.emailVerifyKey(email);
        String saveCode = redisTemplate.opsForValue().get(key);

        if (saveCode == null || !saveCode.equals(inputCode)) {
            log.debug("인증 실패 - 이메일: {}, 입력 코드: {}, 인증 코드: {}", email, inputCode, saveCode);
            throw new CustomException(INVALID_VERIFICATION_CODE);
        }

        redisTemplate.opsForValue().set(key, "true", 10, TimeUnit.MINUTES);

        log.debug("인증 성공 - 인증 상태 10분 간 Redis에 저장 email: {}", email);
    }


    /**
     * 6자리의 무작위 숫자로 이루어진 인증 코드를 생성합니다.
     *
     * @return 6자리 인증 코드 문자열
     */
    private String generateCode() {
        String code = String.valueOf((int) (Math.random() * 900000 + 100000));
        log.debug("인증 코드 생성: {}", code);
        return code;
    }
}