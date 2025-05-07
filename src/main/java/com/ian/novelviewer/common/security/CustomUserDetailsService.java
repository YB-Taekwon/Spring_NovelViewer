package com.ian.novelviewer.common.security;

import com.ian.novelviewer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 로그인 ID로 사용자 정보를 로드하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * 로그인 ID로 사용자 정보를 조회합니다.
     *
     * @param loginId 로그인 ID (username)
     * @return UserDetails 객체 (Spring Security 인증 객체)
     * @throws UsernameNotFoundException 사용자를 찾을 수 없을 경우
     */
    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        return userRepository.findByLoginId(loginId)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
