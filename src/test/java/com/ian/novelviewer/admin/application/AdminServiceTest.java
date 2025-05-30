package com.ian.novelviewer.admin.application;

import com.ian.novelviewer.user.dto.UserDto;
import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.exception.ErrorCode;
import com.ian.novelviewer.user.domain.User;
import com.ian.novelviewer.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ian.novelviewer.common.enums.Role.ROLE_AUTHOR;
import static com.ian.novelviewer.common.enums.Role.ROLE_USER;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    AdminService adminService;

    @Mock
    UserRepository userRepository;

    User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .loginId("testId")
                .password("encodedTestPassword")
                .email("test@example.com")
                .realname("테스트")
                .roles(new ArrayList<>(List.of(ROLE_USER)))
                .build();
    }

    @Test
    @DisplayName("권한 부여 성공")
    void approve_success() {
        // given
        String userId = "testId";

        UserDto.RoleApprovalRequest request = UserDto.RoleApprovalRequest.builder()
                .nickname("테스트닉네임")
                .build();

        when(userRepository.findByLoginId(userId))
                .thenReturn(Optional.of(user));

        // when
        UserDto.RoleApprovalResponse response = adminService.approveRole(userId, request);

        // then
        assertThat(response.getLoginId()).isEqualTo(userId);
        assertThat(response.getNickname()).isEqualTo("테스트닉네임");
        assertThat(response.getRoles()).contains(ROLE_AUTHOR);
    }

    @Test
    @DisplayName("권한 부여 실패 - 존재하지 않는 사용자")
    void approve_fail_user_not_found() {
        // given
        UserDto.RoleApprovalRequest request = UserDto.RoleApprovalRequest.builder()
                .nickname("테스트닉네임")
                .build();

        when(userRepository.findByLoginId("notFound"))
                .thenReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> adminService.approveRole("notFound", request));

        // that
        assertThat(thrown).isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("권한 부여 실패 - 이미 권한이 존재")
    void approve_fail_already_has_role() {
        // given
        String userId = "testId";
        user.addRole(ROLE_AUTHOR);

        UserDto.RoleApprovalRequest request = UserDto.RoleApprovalRequest.builder()
                .nickname("테스트닉네임")
                .build();

        when(userRepository.findByLoginId(userId))
                .thenReturn(Optional.of(user));

        // when
        Throwable thrown = catchThrowable(() -> adminService.approveRole(userId, request));

        // then
        assertThat(thrown).isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_HAS_ROLE);
    }
}