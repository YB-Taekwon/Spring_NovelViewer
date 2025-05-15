package com.ian.novelviewer.user.ui;

import com.ian.novelviewer.common.security.CustomUserDetails;
import com.ian.novelviewer.user.application.UserService;
import com.ian.novelviewer.user.dto.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    /**
     * 로그인한 사용자의 프로필 정보를 조회합니다.
     *
     * @param authentication 인증 객체
     * @return 사용자 프로필 정보 응답
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        String loginId = authentication.getName();
        log.info("프로필 조회 요청 수신 - loginId: {}", loginId);

        UserDto.UserInfoResponse response = userService.getProfile(loginId);

        log.info("프로필 조회 완료 - loginId: {}", response.getLoginId());
        return ResponseEntity.ok(response);
    }


    /**
     * 로그인한 사용자가 북마크한 소설 목록을 페이징하여 조회합니다.
     *
     * @param page 요청할 페이지 번호 (기본값: 0)
     * @param size 한 페이지에 포함될 항목 수 (기본값: 10)
     * @param user 인증된 사용자 정보
     * @return 페이징된 북마크 소설 목록
     */
    @GetMapping("/bookmarks")
    public ResponseEntity<?> getBookmarks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Long userId = user.getUser().getId();
        log.info("북마크 조회 요청 수신 - userId: {}", userId);

        Pageable pageable = PageRequest.of(page, size);

        Page<UserDto.BookmarksResponse> responses = userService.getBookmarks(userId, pageable);

        log.info("북마크 조회 완료 - userId: {}, 결과 수: {}", user.getUsername(), responses.getTotalElements());
        return ResponseEntity.ok(responses);
    }


    /**
     * 로그인한 사용자가 작성한 댓글을 최신순으로 페이징하여 조회합니다.
     *
     * @param page           요청할 페이지 번호 (기본값: 0)
     * @param size           한 페이지에 포함될 항목 수 (기본값: 20)
     * @param authentication 인증 객체 (로그인 ID 추출에 사용)
     * @return 페이징된 사용자 댓글 목록
     */
    @GetMapping("/comments")
    public ResponseEntity<?> getComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        String loginId = authentication.getName();
        log.info("댓글 목록 조회 요청 수신 - loginId: {}, page: {}, size: {}", loginId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<UserDto.CommentsResponse> responses = userService.getComments(loginId, pageable);

        log.info("댓글 목록 조회 완료 - loginId: {}, 총 댓글 수: {}", loginId, responses.getTotalElements());
        return ResponseEntity.ok(responses);
    }


    /**
     * 로그인한 사용자가 작가 권한을 요청합니다.
     *
     * @param request        작가 권한 요청 DTO
     * @param authentication 인증 객체
     * @return 작가 권한 요청 처리 결과
     */
    @PostMapping("/request-role")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> requestRole(
            @RequestBody @Valid UserDto.RoleRequestRequest request,
            Authentication authentication
    ) {
        String loginId = authentication.getName();
        log.info("작가 권한 요청 수신 - 요청자 ID: {}, 요청 필명: {}", loginId, request.getAuthorName());

        UserDto.RoleRequestResponse response = userService.requestRole(request, loginId);

        log.info("작가 권한 요청 처리 완료 - 요청자 ID: {}", loginId);
        return ResponseEntity.ok(response);
    }
}