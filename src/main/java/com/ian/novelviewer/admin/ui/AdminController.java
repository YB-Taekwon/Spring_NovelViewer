package com.ian.novelviewer.admin.ui;

import com.ian.novelviewer.admin.application.AdminService;
import com.ian.novelviewer.user.dto.UserDto;
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

    @PostMapping("/approve-role/{userId}")
    public ResponseEntity<?> approveRole(
            @PathVariable String userId,
            @RequestBody @Valid UserDto.RoleApprovalRequest request,
            Authentication authentication
    ) {
        log.info("작가 권한 승인 요청 수신 - 신청자: {}, 관리자: {}", userId, authentication.getName());
        UserDto.RoleApprovalResponse response = adminService.approveRole(userId, request);

        return ResponseEntity.ok(response);
    }
}
