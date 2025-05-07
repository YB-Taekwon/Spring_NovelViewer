package com.ian.novelviewer.common.config;

import com.ian.novelviewer.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 보안 설정 클래스입니다.
 * JWT 기반 인증을 적용하며, 세션을 사용하지 않는 Stateless 구조로 구성됩니다.
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Spring Security의 필터 체인을 정의합니다.
     * - HTTP Basic, CSRF 비활성화
     * - Stateless 세션 정책
     * - JWT 필터 등록
     *
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain 보안 설정 체인
     * @throws Exception 보안 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // HTTP Basic 인증 비활성화
                .httpBasic(httpBasic -> httpBasic.disable())
                // CSRF 보호 비활성화 (REST API)
                .csrf(csrf -> csrf.disable())
                // 세션을 사용하지 않고 JWT 기반 Stateless 인증 방식 사용
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 인가 정책 설정: /auth/** 경로는 인증 없이 허용, 나머지는 인증 필요
                .authorizeHttpRequests(auth -> auth.requestMatchers("/auth/**").permitAll()
                        .anyRequest().authenticated())
                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AuthenticationManager 빈을 등록합니다.
     * - UserDetailsService 기반 인증 처리를 위해 사용됩니다.
     *
     * @param http HttpSecurity 객체
     * @return AuthenticationManager 인증 매니저
     * @throws Exception 구성 중 오류 발생 시
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManager.class);
    }

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder 빈을 등록합니다.
     * - BCrypt 알고리즘을 사용합니다.
     *
     * @return PasswordEncoder 비밀번호 암호화 도구
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}