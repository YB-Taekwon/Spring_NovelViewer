package com.ian.novelviewer.admin.ui;

import com.ian.novelviewer.admin.application.AdminService;
import com.ian.novelviewer.admin.dto.AdminDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;


    /**
     * 특정 사용자에게 작가 권한을 승인합니다.
     * 관리자만 접근 가능하며, userId에 해당하는 사용자의 권한을 '작가'로 승인 처리합니다.
     *
     * @param userId         승인 대상 사용자의 로그인 ID
     * @param authentication 현재 요청을 수행하는 인증된 관리자 정보
     * @return 승인 결과를 포함한 응답 DTO
     */
    @PostMapping("/approve-role/{userId}")
    public ResponseEntity<?> approveRole(
            @PathVariable String userId,
            Authentication authentication
    ) {
        log.info("작가 권한 승인 요청 수신 - 신청자 ID: {}, 처리 관리자: {}",
                userId, authentication.getName());

        AdminDto.RoleApprovalResponse response = adminService.approveRole(userId);

        log.info("작가 권한 승인 처리 완료 - 승인 대상 ID: {}", userId);
        return ResponseEntity.ok(response);
    }
}
