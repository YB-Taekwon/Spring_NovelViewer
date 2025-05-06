package com.ian.novelviewer.admin.dto;

import com.ian.novelviewer.common.enums.Role;
import com.ian.novelviewer.user.domain.User;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 작가 권한 승인 요청 DTO
 */
public class RoleApprovalDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @NotBlank(message = "아이디를 입력해주세요.")
        private String loginId;

        @NotBlank(message = "작가명을 입력해주세요.")
        private String nickname;
    }

    /**
     * 작가 권한 승인 응답 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private String loginId;
        private String nickname;
        private List<Role> roles;

        /**
         * User Entity 로부터 응답 DTO 를 생성합니다.
         *
         * @param user 권한이 승인된 사용자 정보
         * @return 응답 DTO
         */
        public static RoleApprovalDto.Response from(User user) {
            return Response.builder()
                    .loginId(user.getLoginId())
                    .nickname(user.getNickname())
                    .roles(user.getRoles().stream().toList())
                    .build();
        }
    }
}
