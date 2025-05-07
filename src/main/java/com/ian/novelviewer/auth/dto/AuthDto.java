package com.ian.novelviewer.auth.dto;

import com.ian.novelviewer.common.enums.Role;
import com.ian.novelviewer.user.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

public class AuthDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SignUpRequest {

        @NotBlank(message = "아이디를 입력해주세요.")
        private String loginId;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        private String password;

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        private String email;

        @NotBlank(message = "이름을 입력해주세요.")
        private String realname;

        public static User from(SignUpRequest request) {
            return User.builder()
                    .loginId(request.loginId)
                    .password(request.password)
                    .email(request.email)
                    .realname(request.realname)
                    .roles(List.of(Role.ROLE_USER))
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SignInRequest {

        @NotBlank(message = "아이디를 입력해주세요.")
        private String loginId;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        private String password;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SignUpResponse {

        private String loginId;
        private String email;
        private String realname;
        private List<Role> roles;

        public static AuthDto.SignUpResponse from(User user) {
            return SignUpResponse.builder()
                    .loginId(user.getLoginId())
                    .email(user.getEmail())
                    .realname(user.getRealname())
                    .roles(user.getRoles())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SignInResponse {

        private String loginId;
        private String email;
        private String realname;
        private List<Role> roles;
        private String token;

        public static AuthDto.SignInResponse from(User user, String token) {
            return SignInResponse.builder()
                    .loginId(user.getLoginId())
                    .email(user.getEmail())
                    .realname(user.getRealname())
                    .roles(user.getRoles())
                    .token(token)
                    .build();
        }
    }
}
