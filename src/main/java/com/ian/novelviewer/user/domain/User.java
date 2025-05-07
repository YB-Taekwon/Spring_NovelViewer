package com.ian.novelviewer.user.domain;

import com.ian.novelviewer.common.base.BaseEntity;
import com.ian.novelviewer.common.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String realname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String nickname;

    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    private List<Role> roles = new ArrayList<>();

    public void encodingPassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}