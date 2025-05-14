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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.ian.novelviewer.common.enums.Role.ROLE_ADMIN;
import static com.ian.novelviewer.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final EpisodeRepository episodeRepository;
    private final RedisTemplate<String, String> redisTemplate;


    public Page<CommentDto.CommentResponse> getAllComments(
            Long novelId, Long episodeId, int page, int size
    ) {
        log.debug("[getAllComments] 요청 - novelId={}, episodeId={}, page={}, size={}",
                novelId, episodeId, page, size);

        Pageable pageable = getPageable(page, size);
        log.debug("[getAllComments] Pageable 생성 완료 - sort=likes DESC");

        Page<Comment> comments = commentRepository
                .findAllByEpisode_Novel_NovelIdAndEpisode_EpisodeIdAndParentIsNull
                        (novelId, episodeId, pageable);

        log.debug("[getAllComments] 조회 완료 - 총 댓글 수={}, 총 페이지 수={}",
                comments.getTotalElements(), comments.getTotalPages());

        return comments.map(comment -> {
            Long likes = getLikeCount(comment.getId());
            return CommentDto.CommentResponse.from(comment, likes);
        });
    }

    private static PageRequest getPageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "likes"));
    }


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
        }

        Comment comment = commentRepository.save(
                Comment.builder()
                        .content(request.getContent())
                        .likes(0L)
                        .user(user.getUser())
                        .episode(episode)
                        .parent(parentComment)
                        .build()
        );

        log.debug("[createComment] 댓글 생성 완료 - commentId={}, content={}",
                comment.getId(), comment.getContent());

        return CommentDto.CommentResponse.from(comment, getLikeCount(comment.getId()));
    }


    public CommentDto.CommentResponse getCommet(Long novelId, Long episodeId, Long commentId) {
        log.debug("[getCommet] 요청 - novelId={}, episodeId={}, commentId={}",
                novelId, episodeId, commentId);
        Comment comment = findCommentOrThrow(commentId, episodeId, novelId);

        log.debug("[getCommet] 댓글 조회 성공 - commentId={}", comment.getId());
        return CommentDto.CommentResponse.from(comment, getLikeCount(comment.getId()));
    }


    @Transactional
    public CommentDto.CommentResponse updateComment(
            Long novelId,
            Long episodeId,
            Long commentId,
            CommentDto.UpdateCommentRequest request,
            CustomUserDetails user
    ) {
        log.debug("[updateComment] 요청 - commentId={}, userId={}, content={}",
                commentId, user.getUser().getId(), request.getContent());

        Comment comment = findCommentOrThrow(commentId, episodeId, novelId);

        checkPermissionOrThrow(comment, user);

        if (StringUtils.hasText(request.getContent())) {
            comment.changeContent(request.getContent());
            log.debug("[updateComment] 내용 변경 완료 - commentId={}", commentId);
        }

        return CommentDto.CommentResponse.from(comment, getLikeCount(comment.getId()));
    }


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


    private Comment findCommentOrThrow(Long commentId, Long episodeId, Long novelId) {
        return commentRepository
                .findByIdAndEpisode_EpisodeIdAndEpisode_Novel_NovelId(commentId, episodeId, novelId)
                .orElseThrow(() -> {
                    log.error("[findCommentOrThrow] 댓글 조회 실패 - novelId={}, episodeId={},commentId={}",
                            novelId, episodeId, commentId);
                    throw new CustomException(COMMENT_NOT_FOUND);
                });
    }

    private Episode findEpisodeOrThrow(Long novelId, Long episodeId) {
        return episodeRepository.findByEpisodeIdAndNovel_NovelId(episodeId, novelId)
                .orElseThrow(() -> {
                    log.error("[findEpisodeOrThrow] 회차 조회 실패 - novelId={}, episodeId={}",
                            novelId, episodeId);
                    throw new CustomException(EPISODE_NOT_FOUND);
                });
    }

    private static void checkPermissionOrThrow(Comment comment, CustomUserDetails user) {
        if (!comment.getUser().getLoginId().equals(user.getUsername())) {
            log.error("[checkPermissionOrThrow] 권한 없음 - 작성자={}, 요청자={}",
                    comment.getUser().getLoginId(), user.getUsername());
            throw new CustomException(NO_PERMISSION);
        }
    }


    public Long getLikeCount(Long commentId) {
        String likeKey = RedisKeyUtil.commentLikeKey(commentId);
        Long count = redisTemplate.opsForSet().size(likeKey);

        log.debug("[getLikeCount] commentId={} → 좋아요 수={}", commentId, count);

        return count;
    }


    public boolean hasLiked(Long commentId, Long userId) {
        String likeKey = RedisKeyUtil.commentLikeKey(commentId);
        boolean liked = Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(likeKey, userId.toString()));

        log.debug("[hasLiked] commentId={}, userId={} → hasLiked={}", commentId, userId, liked);

        return liked;
    }


    public void like(Long commentId, Long userId) {
        String likeKey = RedisKeyUtil.commentLikeKey(commentId);

        if (!hasLiked(commentId, userId)) {
            redisTemplate.opsForSet().add(likeKey, userId.toString());
            log.debug("좋아요 완료. userId={}, commentId={}", userId, commentId);
        } else {
            log.debug("[like] 이미 좋아요한 상태 - commentId={}, userId={}", commentId, userId);
        }
    }


    public void unlike(Long commentId, Long userId) {
        String likeKey = RedisKeyUtil.commentLikeKey(commentId);

        if (hasLiked(commentId, userId)) {
            redisTemplate.opsForSet().remove(likeKey, userId.toString());
            log.debug("[unlike] 좋아요 취소 완료 - commentId={}, userId={}", commentId, userId);
        } else {
            log.debug("[unlike] 이미 좋아요하지 않은 상태 - commentId={}, userId={}", commentId, userId);
        }
    }
}