package com.ian.novelviewer.admin.dto;

import com.ian.novelviewer.common.enums.Role;
import com.ian.novelviewer.user.domain.User;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class AdminDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoleApprovalResponse {

        private String loginId;
        private String authorName;
        private boolean roleRequestPending;
        private List<Role> roles;

        public static AdminDto.RoleApprovalResponse from(User user) {
            return RoleApprovalResponse.builder()
                    .loginId(user.getLoginId())
                    .authorName(user.getAuthorName())
                    .roleRequestPending(user.isRoleRequestPending())
                    .roles(user.getRoles().stream().toList())
                    .build();
        }
    }
}