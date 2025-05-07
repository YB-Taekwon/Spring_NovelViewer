package com.ian.novelviewer.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ian.novelviewer.common.base.BaseEntity;
import com.ian.novelviewer.common.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 엔티티 클래스입니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    // 사용자 로그인 ID - 고유값, 필수값
    @Column(nullable = false, unique = true)
    private String loginId;

    // 사용자 비밀번호 (암호화 하여 저장) - JSON 응답에서 제외, 필수값
    @Column(nullable = false)
    private String password;

    // 실명 - 필수값
    @Column(nullable = false)
    private String realname;

    // 이메일 - 고유값, 필수값
    @Column(nullable = false, unique = true)
    private String email;

    // 닉네임 - 작가의 필명으로 사용
    @Column
    private String nickname;

    /**
     * 사용자 권한 목록 (ROLE_USER, ROLE_AUTHOR, ROLE_ADMIN)
     * EAGER 로딩으로 항상 불러옴
     */
    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    private List<Role> roles = new ArrayList<>();
}