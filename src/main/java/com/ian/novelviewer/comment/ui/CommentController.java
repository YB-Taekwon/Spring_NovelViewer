package com.ian.novelviewer.comment.ui;

import com.ian.novelviewer.comment.application.CommentService;
import com.ian.novelviewer.comment.dto.CommentDto;
import com.ian.novelviewer.common.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/novels/{novelId}/episodes/{episodeId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;


    /**
     * 댓글 목록을 조회합니다.
     * Redis 기반 좋아요 수로 정렬되며, 페이지네이션이 적용됩니다.
     * 원 댓글만 조회됩니다.
     *
     * @param novelId   소설 고유번호
     * @param episodeId 회차 고유번호
     * @param page      페이지 번호 (기본값 0)
     * @param size      페이지 크기 (기본값 20)
     * @return 댓글 목록 (Page 형태)
     */
    @GetMapping
    public ResponseEntity<?> getAllComments(
            @PathVariable Long novelId,
            @PathVariable Long episodeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("[GET] 댓글 목록 요청 - novelId={}, episodeId={}, page={}, size={}",
                novelId, episodeId, page, size);

        Page<CommentDto.CommentResponse> responses =
                commentService.getAllComments(novelId, episodeId, page, size);

        return ResponseEntity.ok(responses);
    }


    /**
     * 새로운 댓글을 작성합니다.
     * 대댓글의 경우 parentId를 함께 전달해야 합니다.
     * 대댓글은 1단계 까지만 허용합니다. (자식 댓글은 부모 댓글이 될 수 없습니다.)
     *
     * @param novelId   소설 고유번호
     * @param episodeId 회차 고유번호
     * @param request   댓글 작성 요청 DTO
     * @param user      인증된 사용자 정보
     * @return 작성된 댓글 정보
     */
    @PostMapping
    public ResponseEntity<?> createComment(
            @PathVariable Long novelId,
            @PathVariable Long episodeId,
            @RequestBody @Valid CommentDto.CreateCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("[POST] 댓글 작성 요청 - novelId={}, episodeId={}, userId={}, content={}",
                novelId, episodeId, user.getUser().getId(), request.getContent());

        CommentDto.CommentResponse response =
                commentService.createComment(novelId, episodeId, request, user);

        return ResponseEntity.ok(response);
    }


    /**
     * 단일 댓글 상세 정보를 조회합니다.
     * 대댓글이 함께 조회됩니다.
     *
     * @param novelId   소설 고유번호
     * @param episodeId 회차 고유번호
     * @param commentId 댓글 고유 ID
     * @return 댓글 응답 DTO
     */
    @GetMapping("/{commentId}")
    public ResponseEntity<?> getComment(
            @PathVariable Long novelId,
            @PathVariable Long episodeId,
            @PathVariable Long commentId
    ) {
        log.info("[GET] 단일 댓글 조회 요청 - novelId={}, episodeId={}, commentId={}",
                novelId, episodeId, commentId);

        CommentDto.CommentInfoResponse response = commentService.getCommet(novelId, episodeId, commentId);

        return ResponseEntity.ok(response);
    }


    /**
     * 댓글을 수정합니다.
     * 작성자만 수정할 수 있습니다.
     *
     * @param novelId        소설 고유번호
     * @param episodeId      회차 고유번호
     * @param commentId      댓글 고유 ID
     * @param request        수정 요청 DTO
     * @param authentication 인증 객체
     * @return 수정된 댓글 응답 DTO
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable Long novelId,
            @PathVariable Long episodeId,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentDto.UpdateCommentRequest request,
            Authentication authentication
    ) {
        String loginId = authentication.getName();
        log.info("[PUT] 댓글 수정 요청 - novelId={}, episodeId={}, commentId={}, userId={}, content={}",
                novelId, episodeId, commentId, loginId, request.getContent());

        CommentDto.CommentResponse response =
                commentService.updateComment(novelId, episodeId, commentId, request, loginId);

        return ResponseEntity.ok(response);
    }


    /**
     * 댓글을 삭제합니다.
     * 작성자 또는 관리자만 삭제할 수 있습니다.
     *
     * @param novelId   소설 고유번호
     * @param episodeId 회차 고유번호
     * @param commentId 댓글 고유 ID
     * @param user      인증된 사용자 정보
     * @return 삭제 완료 메시지
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long novelId,
            @PathVariable Long episodeId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("[DELETE] 댓글 삭제 요청 - novelId={}, episodeId={}, commentId={}, userId={}",
                novelId, episodeId, commentId, user.getUser().getId());

        commentService.deleteComment(novelId, episodeId, commentId, user);

        return ResponseEntity.ok("댓글이 성공적으로 삭제되었습니다.");
    }


    /**
     * 댓글에 좋아요를 추가합니다.
     * 이미 좋아요한 경우 중복 반영되지 않습니다.
     *
     * @param novelId   소설 고유번호
     * @param episodeId 회차 고유번호
     * @param commentId 댓글 고유 ID
     * @param user      인증된 사용자 정보
     * @return 상태 200 OK (본문 없음)
     */
    @PostMapping("/{commentId}/likes")
    public ResponseEntity<Void> like(
            @PathVariable Long novelId,
            @PathVariable Long episodeId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Long userId = getUserId(user);
        log.info("[POST] 댓글 좋아요 요청 - commentId={}, userId={}", commentId, userId);

        commentService.like(commentId, episodeId, novelId, userId);

        return ResponseEntity.ok().build();
    }


    /**
     * 댓글의 좋아요를 취소합니다.
     * 아직 좋아요하지 않았다면 반영되지 않습니다.
     *
     * @param novelId   소설 고유번호
     * @param episodeId 회차 고유번호
     * @param commentId 댓글 고유 ID
     * @param user      인증된 사용자 정보
     * @return 상태 200 OK (본문 없음)
     */
    @DeleteMapping("/{commentId}/likes")
    public ResponseEntity<Void> unlike(
            @PathVariable Long novelId,
            @PathVariable Long episodeId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Long userId = getUserId(user);
        log.info("[DELETE] 댓글 좋아요 취소 요청 - commentId={}, userId={}", commentId, userId);

        commentService.unlike(commentId, episodeId, novelId, userId);

        return ResponseEntity.ok().build();
    }


    /**
     * 사용자의 고유 ID를 추출합니다.
     */
    private static Long getUserId(CustomUserDetails user) {
        return user.getUser().getId();
    }
}