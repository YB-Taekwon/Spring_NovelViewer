package com.ian.novelviewer.auth.dto;

import com.ian.novelviewer.common.enums.Role;
import com.ian.novelviewer.user.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

public class AuthDto {

    /**
     * 회원가입 요청 DTO
     */
    @Getter
    @Setter
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

        /**
         * 회원가입 DTO를 User Entity로 변환합니다.
         *
         * @param request 회원가입 DTO
         * @return User Entity
         */
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

    /**
     * 회원가입/로그인 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SignUpResponse {
        private String loginId;
        private String email;
        private String realname;
        private List<Role> roles;
        private String token;

        /**
         * User Entity 와 토큰을 기반으로 응답 DTO 를 생성합니다.
         *
         * @param user User Entity
         * @return 응답 DTO
         */
        public static AuthDto.SignUpResponse from(User user) {
            return SignUpResponse.builder()
                    .loginId(user.getLoginId())
                    .email(user.getEmail())
                    .realname(user.getRealname())
                    .roles(user.getRoles())
                    .build();
        }
    }
}
