package com.ian.novelviewer.admin.ui;

import com.ian.novelviewer.admin.application.AdminService;
import com.ian.novelviewer.admin.dto.RoleApprovalDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 권한 API 컨트롤러입니다.
 * 작가 권한 승인 등의 작업을 담당합니다.
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * 작가 권한을 승인합니다.
     *
     * @param request        작가 권한 승인 요청 DTO (승인을 요청한 사용자 정보)
     * @param authentication 관리자 인증 정보
     * @return 승인된 사용자 정보 (로그인 아이디, 작가명, 권한)
     */
    @PostMapping("/assign-role")
    public ResponseEntity<?> approveRoles(
            @RequestBody @Valid RoleApprovalDto.Request request,
            Authentication authentication
    ) {
        log.info("작가 권한 승인 요청 수신 - 신청자: {}, 관리자: {}", request.getLoginId(), authentication.getName());
        RoleApprovalDto.Response response = adminService.approveRoles(request);

        return ResponseEntity.ok(response);
    }
}
