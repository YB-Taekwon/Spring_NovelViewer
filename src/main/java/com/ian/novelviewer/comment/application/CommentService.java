package com.ian.novelviewer.comment.application;

import com.ian.novelviewer.comment.domain.Comment;
import com.ian.novelviewer.comment.domain.CommentRepository;
import com.ian.novelviewer.comment.dto.CommentDto;
import com.ian.novelviewer.common.enums.Role;
import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.redis.RedisKeyUtil;
import com.ian.novelviewer.common.security.CustomUserDetails;
import com.ian.novelviewer.episode.domain.Episode;
import com.ian.novelviewer.episode.domain.EpisodeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

import static com.ian.novelviewer.common.enums.Role.ROLE_ADMIN;
import static com.ian.novelviewer.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final EpisodeRepository episodeRepository;
    private final RedisTemplate<String, String> redisTemplate;


    /**
     * 특정 회차의 댓글 목록을 Redis의 좋아요 수 기준으로 정렬하여 페이징된 형태로 반환합니다.
     *
     * @param novelId   소설 고유번호
     * @param episodeId 회차 고유번호
     * @param page      요청 페이지 번호 (0부터 시작)
     * @param size      페이지당 항목 수
     * @return 정렬된 댓글 목록의 페이징 결과
     */
    public Page<CommentDto.CommentResponse> getAllComments(
            Long novelId, Long episodeId, int page, int size
    ) {
        log.debug("[getAllComments] 요청 - novelId={}, episodeId={}, page={}, size={}",
                novelId, episodeId, page, size);

        Pageable pageable = PageRequest.of(page, size);

        List<Comment> comments = commentRepository
                .findAllByEpisode_Novel_NovelIdAndEpisode_EpisodeIdAndParentIsNull(novelId, episodeId, pageable)
                .getContent();

        log.debug("[getAllCommentsSortedByLikes] 댓글 수 = {}", comments.size());

        List<CommentDto.CommentResponse> sortedResponses = comments.stream()
                .map(comment -> {
                    Long likes = getLikeCount(comment.getId());
                    return CommentDto.CommentResponse.from(comment, likes != null ? likes : 0L);
                })
                .sorted(Comparator.comparingLong(CommentDto.CommentResponse::getLikes).reversed())
                .toList();

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, sortedResponses.size());
        List<CommentDto.CommentResponse> paged = sortedResponses.subList(fromIndex, toIndex);

        log.debug("[getAllCommentsSortedByLikes] 정렬 완료 - 반환 범위: {} ~ {}", fromIndex, toIndex);

        return new PageImpl<>(paged, PageRequest.of(page, size), sortedResponses.size());
    }


    /**
     * 댓글을 새로 생성합니다.
     * 대댓글은 한 단계만 작성이 가능합니다,
     *
     * @param novelId   소설 고유번호
     * @param episodeId 회차 고유번호
     * @param request   댓글 생성 요청 DTO
     * @param user      인증된 사용자 정보
     * @return 생성된 댓글 정보 DTO
     */
    @Transactional
    public CommentDto.CommentResponse createComment(
            Long novelId,
            Long episodeId,
            CommentDto.CreateCommentRequest request,
            CustomUserDetails user
    ) {
        log.debug("[createComment] 요청 - novelId={}, episodeId={}, userId={}, parentId={}, content={}",
                novelId, episodeId, user.getUser().getId(), request.getParentId(), request.getContent());

        Episode episode = findEpisodeOrThrow(novelId, episodeId);
        log.debug("[createComment] 회차 확인 완료 - episodeId={}, title={}",
                episode.getId(), episode.getTitle());

        Comment parentComment = null;
        if (request.getParentId() != null) {
            parentComment = findCommentOrThrow(request.getParentId(), episodeId, novelId);
            log.debug("[createComment] 부모 댓글 확인 완료 - parentId={}", parentComment.getId());

            if (parentComment.getParent() != null) {
                log.warn("[createComment] 2단계 이상 대댓글 시도 차단 - parentId={}", parentComment.getId());
                throw new CustomException(INVALID_COMMENT);
            }
        }

        Comment comment = commentRepository.save(
                Comment.builder()
                        .content(request.getContent())
                        .user(user.getUser())
                        .episode(episode)
                        .parent(parentComment)
                        .build()
        );

        log.debug("[createComment] 댓글 생성 완료 - commentId={}, content={}",
                comment.getId(), comment.getContent());

        return CommentDto.CommentResponse.from(comment, getLikeCount(comment.getId()));
    }


    /**
     * 단일 댓글 정보를 조회합니다.
     *
     * @param novelId   소설 고유번호
     * @param episodeId 회차 고유번호
     * @param commentId 댓글 고유 ID
     * @return 댓글 응답 DTO
     */
    public CommentDto.CommentInfoResponse getCommet(Long novelId, Long episodeId, Long commentId) {
        log.debug("[getCommet] 요청 - novelId={}, episodeId={}, commentId={}",
                novelId, episodeId, commentId);
        Comment comment = findCommentOrThrow(commentId, episodeId, novelId);

        log.debug("[getCommet] 댓글 조회 성공 - commentId={}", comment.getId());
        return CommentDto.CommentInfoResponse.from(comment, getLikeCount(comment.getId()));
    }


    /**
     * 댓글을 수정합니다.
     * 댓글의 작성자만 수정할 수 있습니다.
     *
     * @param novelId   소설 고유번호
     * @param episodeId 회차 고유번호
     * @param commentId 댓글 고유 ID
     * @param request   수정할 내용 DTO
     * @param loginId   요청자 로그인 ID
     * @return 수정된 댓글 DTO
     */
    @Transactional
    public CommentDto.CommentResponse updateComment(
            Long novelId,
            Long episodeId,
            Long commentId,
            CommentDto.UpdateCommentRequest request,
            String loginId
    ) {
        log.debug("[updateComment] 요청 - commentId={}, userId={}, content={}",
                commentId, loginId, request.getContent());

        Comment comment = findCommentOrThrow(commentId, episodeId, novelId);

        if (!comment.getUser().getLoginId().equals(loginId)) {
            log.error("[checkPermissionOrThrow] 권한 없음 - 작성자={}, 요청자={}",
                    comment.getUser().getLoginId(), loginId);
            throw new CustomException(NO_PERMISSION);
        }

        if (StringUtils.hasText(request.getContent())) {
            comment.changeContent(request.getContent());
            log.debug("[updateComment] 내용 변경 완료 - commentId={}", commentId);
        }

        return CommentDto.CommentResponse.from(comment, getLikeCount(comment.getId()));
    }


    /**
     * 댓글을 삭제합니다.
     * 댓글의 작성자 또는 관리자만 삭제할 수 있습니다.
     *
     * @param novelId   소설 고유번호
     * @param episodeId 회차 고유번호
     * @param commentId 댓글 고유 ID
     * @param user      요청 사용자 정보
     */
    @Transactional
    public void deleteComment(Long novelId, Long episodeId, Long commentId, CustomUserDetails user) {
        log.debug("[deleteComment] 요청 - novelId={}, episodeId={}, commentId={}, 요청자 loginId={}",
                novelId, episodeId, commentId, user.getUsername());

        Comment comment = findCommentOrThrow(commentId, episodeId, novelId);
        log.debug("[deleteComment] 댓글 조회 성공 - commentId={}, 작성자={}",
                comment.getId(), comment.getUser().getLoginId());

        boolean isAdmin = user.getUser().getRoles().contains(ROLE_ADMIN);
        boolean isWriter = comment.getUser().getLoginId().equals(user.getUsername());
        log.debug("[deleteComment] 권한 체크 - isAdmin={}, isWriter={}", isAdmin, isWriter);

        if (!isAdmin && !isWriter) {
            log.error("[deleteComment] 권한 없음 - 요청자={}, 댓글 작성자={}",
                    user.getUsername(), comment.getUser().getLoginId());
            throw new CustomException(NO_PERMISSION);
        }

        commentRepository.delete(comment);

        log.debug("[deleteComment] 삭제 권한 확인 완료 - commentId={} 삭제 진행 가능", commentId);
    }


    /**
     * 댓글 ID, 회차 고유번호, 소설 고유번호로 댓글을 조회합니다.
     * 일치하지 않으면 예외를 던집니다.
     *
     * @param commentId 댓글 고유 ID
     * @param episodeId 회차 고유번호
     * @param novelId   소설 고유번호
     * @return 댓글 엔티티
     */
    private Comment findCommentOrThrow(Long commentId, Long episodeId, Long novelId) {
        return commentRepository
                .findByIdAndEpisode_EpisodeIdAndEpisode_Novel_NovelId(commentId, episodeId, novelId)
                .orElseThrow(() -> {
                    log.error("[findCommentOrThrow] 댓글 조회 실패 - novelId={}, episodeId={},commentId={}",
                            novelId, episodeId, commentId);
                    throw new CustomException(COMMENT_NOT_FOUND);
                });
    }


    /**
     * 소설 고유번호와 회차 고유번호로 회차 정보를 조회합니다.
     * 없으면 예외를 던집니다.
     *
     * @param novelId   소설 고유번호
     * @param episodeId 회차 고유번호
     * @return 회차 엔티티
     */
    private Episode findEpisodeOrThrow(Long novelId, Long episodeId) {
        return episodeRepository.findByEpisodeIdAndNovel_NovelId(episodeId, novelId)
                .orElseThrow(() -> {
                    log.error("[findEpisodeOrThrow] 회차 조회 실패 - novelId={}, episodeId={}",
                            novelId, episodeId);
                    throw new CustomException(EPISODE_NOT_FOUND);
                });
    }


    /**
     * 댓글 ID를 기준으로 해당 댓글의 좋아요 수를 Redis에서 조회합니다.
     *
     * @param commentId 댓글 고유 ID
     * @return 좋아요 수 (없으면 0)
     */
    public Long getLikeCount(Long commentId) {
        String likeKey = RedisKeyUtil.commentLikeKey(commentId);
        Long count = redisTemplate.opsForSet().size(likeKey);

        log.debug("[getLikeCount] commentId={} → 좋아요 수={}", commentId, count);

        return count;
    }


    /**
     * 특정 사용자가 해당 댓글에 좋아요를 눌렀는지 여부를 확인합니다.
     *
     * @param commentId 댓글 고유 ID
     * @param userId    사용자 고유 ID
     * @return true = 이미 좋아요 함 / false = 아직 안 함
     */
    public boolean hasLiked(Long commentId, Long userId) {
        String likeKey = RedisKeyUtil.commentLikeKey(commentId);
        boolean liked = Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(likeKey, userId.toString()));

        log.debug("[hasLiked] commentId={}, userId={} → hasLiked={}", commentId, userId, liked);

        return liked;
    }


    /**
     * 댓글에 좋아요를 추가합니다.
     * 이미 눌렀다면 중복 반영되지 않습니다.
     *
     * @param commentId 댓글 고유 ID
     * @param userId    사용자 고유 ID
     */
    public void like(Long commentId, Long episodeId, Long novelId, Long userId) {
        findCommentOrThrow(commentId, episodeId, novelId);
        String likeKey = RedisKeyUtil.commentLikeKey(commentId);

        if (!hasLiked(commentId, userId)) {
            redisTemplate.opsForSet().add(likeKey, userId.toString());
            log.debug("좋아요 완료. userId={}, commentId={}", userId, commentId);
        } else {
            log.debug("[like] 이미 좋아요한 상태 - commentId={}, userId={}", commentId, userId);
        }
    }


    /**
     * 댓글에 눌렀던 좋아요를 취소합니다.
     * 좋아요하지 않은 상태라면 무시됩니다.
     *
     * @param commentId 댓글 고유 ID
     * @param userId    사용자 고유 ID
     */
    public void unlike(Long commentId, Long episodeId, Long novelId, Long userId) {
        String likeKey = RedisKeyUtil.commentLikeKey(commentId);

        if (hasLiked(commentId, userId)) {
            redisTemplate.opsForSet().remove(likeKey, userId.toString());
            log.debug("[unlike] 좋아요 취소 완료 - commentId={}, userId={}", commentId, userId);
        } else {
            log.debug("[unlike] 이미 좋아요하지 않은 상태 - commentId={}, userId={}", commentId, userId);
        }
    }
}