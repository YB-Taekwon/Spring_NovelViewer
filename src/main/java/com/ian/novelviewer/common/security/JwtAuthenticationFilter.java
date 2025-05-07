package com.ian.novelviewer.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * JWT 인증 필터입니다.
 * 매 요청마다 JWT 를 검사하여 인증 정보를 SecurityContext 에 설정합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String TOKEN_HEADER = AUTHORIZATION;
    public static final String TOKEN_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;

    /**
     * 요청 필터링 로직입니다.
     * 유효한 JWT가 있을 경우 인증 처리를 수행합니다.
     *
     * @param request     현재 요청 (헤더, 파라미터 등 포함)
     * @param response    현재 응답 (필터에서 응답 수정 가능)
     * @param filterChain 다음 필터로 요청을 전달하는 체인
     * @throws ServletException 서블릿 처리 중 발생할 수 있는 예외
     * @throws IOException      I/O 관련 예외
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
            Authentication authentication = getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청 헤더에서 JWT 를 추출합니다.
     *
     * @param request 현재 요청
     * @return 추출된 JWT 문자열 또는 null
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(TOKEN_HEADER);

        if (!ObjectUtils.isEmpty(header) && header.startsWith(TOKEN_PREFIX))
            return header.substring(TOKEN_PREFIX.length());

        return null;
    }


    /**
     * JWT 에서 인증 객체를 생성합니다.
     *
     * @param token JWT
     * @return Authentication 인증 객체
     */
    private Authentication getAuthentication(String token) {
        String loginId = jwtProvider.getLoginId(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(loginId);

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
