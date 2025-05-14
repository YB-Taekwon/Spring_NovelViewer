package com.ian.novelviewer.comment.ui;

import com.ian.novelviewer.comment.application.CommentService;
import com.ian.novelviewer.comment.dto.CommentDto;
import com.ian.novelviewer.common.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/novels/{novelId}/episodes/{episodeId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;


    @GetMapping
    public ResponseEntity<?> getAllComments(
            @PathVariable Long novelId,
            @PathVariable Long episodeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("[GET] 댓글 목록 요청 - novelId={}, episodeId={}, page={}, size={}",
                novelId, episodeId, page, size);

        Page<CommentDto.CommentResponse> responses =
                commentService.getAllComments(novelId, episodeId, page, size);

        return ResponseEntity.ok(responses);
    }


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


    @GetMapping("/{commentId}")
    public ResponseEntity<?> getComment(
            @PathVariable Long novelId,
            @PathVariable Long episodeId,
            @PathVariable Long commentId
    ) {
        log.info("[GET] 단일 댓글 조회 요청 - novelId={}, episodeId={}, commentId={}",
                novelId, episodeId, commentId);

        CommentDto.CommentResponse response = commentService.getCommet(novelId, episodeId, commentId);

        return ResponseEntity.ok(response);
    }


    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable Long novelId,
            @PathVariable Long episodeId,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentDto.UpdateCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("[PUT] 댓글 수정 요청 - novelId={}, episodeId={}, commentId={}, userId={}, content={}",
                novelId, episodeId, commentId, user.getUser().getId(), request.getContent());

        CommentDto.CommentResponse response =
                commentService.updateComment(novelId, episodeId, commentId, request, user);

        return ResponseEntity.ok(response);
    }


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


    @GetMapping("/{commentId}/like")
    public ResponseEntity<?> getLikeCount(
            @PathVariable Long novelId,
            @PathVariable Long episodeId,
            @PathVariable Long commentId
    ) {
        log.info("[GET] 댓글 좋아요 수 조회 - commentId={}", commentId);

        Long count = commentService.getLikeCount(commentId);
        return ResponseEntity.ok(count);
    }


    @PostMapping("/{commentId}/like")
    public ResponseEntity<Void> like(
            @PathVariable Long novelId,
            @PathVariable Long episodeId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("[POST] 댓글 좋아요 요청 - commentId={}, userId={}", commentId, user.getUser().getId());

        commentService.like(commentId, user.getUser().getId());

        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<Void> unlike(
            @PathVariable Long novelId,
            @PathVariable Long episodeId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("[DELETE] 댓글 좋아요 취소 요청 - commentId={}, userId={}", commentId, user.getUser().getId());

        commentService.unlike(commentId, user.getUser().getId());

        return ResponseEntity.ok().build();
    }
}