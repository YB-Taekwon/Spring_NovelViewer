package com.ian.novelviewer.common.security;

import com.ian.novelviewer.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Spring Security에서 사용하는 인증 정보 객체입니다.
 * 내부적으로 User Entity 를 감싸고 있습니다.
 */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    /**
     * 권한 목록 반환을 반환합니다.
     * Role enum을 기반으로 ROLE_ 접두어가 붙은 권한 생성합니다.
     *
     * @return List<Role> 권한 정보 리스트
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles()
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }

    /**
     * 사용자 비밀번호를 반환합니다.
     *
     * @return password 비밀번호
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * 사용자 로그인 ID를 반환합니다.
     *
     * @return loginId 로그인 ID
     */
    @Override
    public String getUsername() {
        return user.getLoginId();
    }

    /**
     * 계정 만료 여부를 설정합니다.
     *
     * @return true (만료되지 않음)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 계정 잠김 여부를 설정합니다.
     *
     * @return true (잠기지 않음)
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 자격 증명 (비밀번호) 만료 여부를 설정합니다.
     *
     * @return true (만료되지 않음)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 사용자 활성화 여부를 설정합니다.
     *
     * @return true (활성 사용자)
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
