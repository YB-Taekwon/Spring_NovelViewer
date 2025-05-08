package com.ian.novelviewer.user.dto;

import com.ian.novelviewer.common.enums.Role;
import com.ian.novelviewer.user.domain.User;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class UserDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoleApprovalRequest {

        @NotBlank(message = "작가명을 입력해주세요.")
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoleApprovalResponse {

        private String loginId;
        private String nickname;
        private List<Role> roles;

        public static UserDto.RoleApprovalResponse from(User user) {
            return RoleApprovalResponse.builder()
                    .loginId(user.getLoginId())
                    .nickname(user.getNickname())
                    .roles(user.getRoles().stream().toList())
                    .build();
        }
    }
}