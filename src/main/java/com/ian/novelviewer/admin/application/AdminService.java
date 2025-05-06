package com.ian.novelviewer.admin.application;

import com.ian.novelviewer.admin.dto.RoleApprovalDto;
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

/**
 * 관리자 서비스 클래스입니다.
 * 사용자에게 작가 권한을 부여하는 로직을 포함합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    /**
     * 사용자의 작가 권한을 승인합니다.
     *
     * @param request 작가 권한 승인 요청 DTO (승인을 요청한 사용자 정보)
     * @return 승인된 사용자 정보 (로그인 아이디, 작가명, 권한)
     */
    @Transactional
    public RoleApprovalDto.Response approveRoles(RoleApprovalDto.Request request) {
        log.info("작가 권한 승인 요청 처리 - 신청자: {}", request.getLoginId());
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> {
                    log.warn("작가 등록 실패 - 존재하지 않는 사용자: {}", request.getLoginId());
                    return new CustomException(USER_NOT_FOUND);
                });

        if (user.getRoles().contains(ROLE_AUTHOR)) {
            log.warn("작가 등록 실패 - 이미 등록된 작가: {}", request.getLoginId());
            throw new CustomException(ALREADY_HAS_ROLE);
        }

        user.setNickname(request.getNickname());
        user.addRole(ROLE_AUTHOR);

        log.info("작가 권한 승인 완료");

        return RoleApprovalDto.Response.from(user);
    }
}
