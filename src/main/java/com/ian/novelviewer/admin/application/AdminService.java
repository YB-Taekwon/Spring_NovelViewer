package com.ian.novelviewer.admin.application;

import com.ian.novelviewer.admin.dto.AdminDto;
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


    /**
     * 지정된 사용자 ID에 대해 '작가' 권한을 승인합니다.
     * <p>
     * - 사용자가 존재하지 않으면 예외를 발생시킵니다.
     * - 이미 '작가' 권한이 있는 경우 중복 승인을 방지하기 위해 예외를 발생시킵니다.
     * - 승인 후, 사용자 객체에 작가 권한을 부여하고 응답 DTO로 반환합니다.
     *
     * @param userId 권한 승인을 요청한 사용자 ID
     * @return 승인된 사용자 정보를 포함한 응답 DTO
     * @throws CustomException USER_NOT_FOUND   - 사용자 조회 실패 시
     *                         ALREADY_HAS_ROLE - 이미 작가 권한이 있는 경우
     */
    @Transactional
    public AdminDto.RoleApprovalResponse approveRole(String userId) {
        log.debug("작가 권한 승인 처리 시작 - 대상 사용자 ID: {}", userId);

        User user = userRepository.findByLoginId(userId)
                .orElseThrow(() -> {
                    log.error("작가 권한 승인 실패 - 존재하지 않는 사용자: {}", userId);
                    return new CustomException(USER_NOT_FOUND);
                });

        log.debug("사용자 조회 성공 - 사용자 ID: {}, 현재 권한: {}", userId, user.getRoles());

        if (user.getRoles().contains(ROLE_AUTHOR)) {
            log.error("작가 권한 승인 불가 - 이미 작가 권한이 있음: {}", userId);
            throw new CustomException(ALREADY_HAS_ROLE);
        }

        user.approveAuthorRole();

        log.debug("작가 권한 승인 완료 - 필명: {}, 권한: {}", user.getAuthorName(), user.getRoles());
        return AdminDto.RoleApprovalResponse.from(user);
    }
}