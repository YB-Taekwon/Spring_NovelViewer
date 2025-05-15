package com.ian.novelviewer.user.domain;

import com.ian.novelviewer.common.base.BaseEntity;
import com.ian.novelviewer.common.enums.Role;
import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.exception.ErrorCode;
import com.ian.novelviewer.novel.domain.Novel;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import static com.ian.novelviewer.common.enums.Role.ROLE_AUTHOR;
import static com.ian.novelviewer.common.exception.ErrorCode.ALREADY_HAS_ROLE;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String userName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String authorName;

    @Column
    private String requestAuthorName;

    @Column
    private boolean roleRequestPending;

    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    private List<Role> roles = new ArrayList<>();

    @OneToMany(mappedBy = "author")
    private List<Novel> novels = new ArrayList<>();

    public void encodingPassword(String encodedPassword) {
        password = encodedPassword;
    }

    public void requestAuthorRole(String authorName) {
        this.requestAuthorName = authorName;
        this.roleRequestPending = true;
    }

    public void addAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public void addRole(Role role) {
        if (!roles.contains(role))
            roles.add(role);
    }

    public void approveAuthorRole() {
        if (!this.roleRequestPending) throw new CustomException(ALREADY_HAS_ROLE);
        this.addAuthorName(this.requestAuthorName);
        this.addRole(ROLE_AUTHOR);
        this.roleRequestPending = false;
        this.requestAuthorName = null;
    }
}