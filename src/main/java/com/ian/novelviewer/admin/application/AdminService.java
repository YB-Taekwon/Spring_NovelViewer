package com.ian.novelviewer.admin.application;

import com.ian.novelviewer.user.dto.UserDto;
import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.user.domain.User;
import com.ian.novelviewer.user.domain.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.ian.novelviewer.common.enums.Role.ROLE_AUTHOR;
import static com.ian.novelviewer.common.exception.ErrorCode.ALREADY_HAS_ROLE;
import static com.ian.novelviewer.common.exception.ErrorCode.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    @Transactional
    public UserDto.RoleApprovalResponse approveRole(String userId, UserDto.RoleApprovalRequest request) {
        log.info("작가 권한 승인 요청 처리 - 신청자: {}", userId);
        User user = userRepository.findByLoginId(userId)
                .orElseThrow(() -> {
                    log.warn("작가 등록 실패 - 존재하지 않는 사용자: {}", userId);
                    return new CustomException(USER_NOT_FOUND);
                });

        if (user.getRoles().contains(ROLE_AUTHOR)) {
            log.warn("작가 등록 실패 - 이미 등록된 작가: {}", userId);
            throw new CustomException(ALREADY_HAS_ROLE);
        }

        user.addAuthorName(request.getNickname());
        user.addRole(ROLE_AUTHOR);

        log.info("작가 권한 승인 완료");

        return UserDto.RoleApprovalResponse.from(user);
    }
}