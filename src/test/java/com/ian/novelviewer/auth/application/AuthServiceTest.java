package com.ian.novelviewer.auth.application;

import com.ian.novelviewer.auth.dto.AuthDto;
import com.ian.novelviewer.common.security.JwtProvider;
import com.ian.novelviewer.user.domain.User;
import com.ian.novelviewer.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static com.ian.novelviewer.common.enums.Role.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        // given
        AuthDto.SignUp request = AuthDto.SignUp.builder()
                .loginId("hong123")
                .password("pass1234")
                .email("hong@example.com")
                .realname("홍길동")
                .build();

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .loginId("hong123")
                .password(encodedPassword)
                .email("hong@example.com")
                .realname("홍길동")
                .roles(List.of(ROLE_USER))
                .build();

        given(userRepository.existsByLoginId("hong123")).willReturn(false);
        given(userRepository.existsByEmail("hong@example.com")).willReturn(false);
        given(passwordEncoder.encode("pass1234")).willReturn(encodedPassword);
        given(userRepository.save(any(User.class))).willReturn(user);
        given(jwtProvider.generateToken("hong123", List.of(ROLE_USER))).willReturn("testToken");

        // when
        AuthDto.AuthResponse response = authService.signup(request);

        // then
        assertThat(response.getLoginId()).isEqualTo("hong123");
        assertThat(response.getEmail()).isEqualTo("hong@example.com");
        assertThat(response.getRealname()).isEqualTo("홍길동");
        assertThat(response.getRoles()).containsExactly(ROLE_USER);
        assertThat(response.getToken()).isEqualTo("testToken");
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 아이디")
    void signup_fail_duplicate_loginId() {
        // given
        given(userRepository.existsByLoginId("hong123")).willReturn(true);

        AuthDto.SignUp request = AuthDto.SignUp.builder()
                .loginId("hong123") // 중복 아이디
                .password("pass1234")
                .email("new@example.com")
                .realname("홍길순")
                .build();

        // when & then
        assertThrows(RuntimeException.class, () -> authService.signup(request));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 이메일")
    void signup_fail_duplicate_email() {
        // given
        given(userRepository.existsByEmail("hong@example.com")).willReturn(true);

        AuthDto.SignUp request = AuthDto.SignUp.builder()
                .loginId("hong1234")
                .password("pass1234")
                .email("hong@example.com") // 중복 이메일
                .realname("홍길순")
                .build();

        // when & then
        assertThrows(RuntimeException.class, () -> authService.signup(request));
    }

    /////////////////////////////// 로그인 ///////////////////////////////

    @Test
    @DisplayName("로그인 성공")
    void signin_success() {
        // given
        AuthDto.SignIn request = AuthDto.SignIn.builder()
                .loginId("hong123")
                .password("pass1234")
                .build();

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .loginId("hong123")
                .password(encodedPassword)
                .email("hong@example.com")
                .realname("홍길동")
                .roles(List.of(ROLE_USER))
                .build();

        given(userRepository.findByLoginId("hong123")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("pass1234", encodedPassword)).willReturn(true);
        given(jwtProvider.generateToken("hong123", List.of(ROLE_USER))).willReturn("testToken");

        // when
        AuthDto.AuthResponse response = authService.signin(request);

        // then
        assertThat(response.getLoginId()).isEqualTo("hong123");
        assertThat(response.getEmail()).isEqualTo("hong@example.com");
        assertThat(response.getRealname()).isEqualTo("홍길동");
        assertThat(response.getRoles()).containsExactly(ROLE_USER);
        assertThat(response.getToken()).isEqualTo("testToken");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 아이디")
    void signin_fail_invalid_id() {
        // given
        given(userRepository.findByLoginId("invalidId")).willReturn(Optional.empty());

        AuthDto.SignIn request = AuthDto.SignIn.builder()
                .loginId("invalidId")
                .password("pass1234")
                .build();

        // when & then
        assertThrows(RuntimeException.class, () -> authService.signin(request));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void signin_fail_wrong_password() {
        // given
        AuthDto.SignIn request = AuthDto.SignIn.builder()
                .loginId("hong123")
                .password("pass1234")
                .build();

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .loginId("hong123")
                .password(encodedPassword)
                .email("hong@example.com")
                .realname("홍길동")
                .roles(List.of(ROLE_USER))
                .build();

        given(userRepository.findByLoginId("hong123")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPassword", encodedPassword)).willReturn(false);

        // when & then
        assertThrows(RuntimeException.class, () -> authService.signin(request));
    }
}