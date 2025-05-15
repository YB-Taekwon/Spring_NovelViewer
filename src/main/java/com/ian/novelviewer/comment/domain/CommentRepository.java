package com.ian.novelviewer.comment.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    /**
     * 특정 작품(novelId)과 회차(episodeId)에 해당하는 부모 댓글들을 페이징 하여 조회합니다.
     */
    Page<Comment> findAllByEpisode_Novel_NovelIdAndEpisode_EpisodeIdAndParentIsNull
    (Long novelId, Long episodeId, Pageable pageable);

    /**
     * 작품, 회차, 댓글 ID가 모두 일치하는 댓글을 단건으로 조회합니다.
     */
    Optional<Comment> findByIdAndEpisode_EpisodeIdAndEpisode_Novel_NovelId
    (Long novelId, Long episodeId, Long commentId);

    /**
     * 특정 사용자의 로그인 ID(loginId)를 기준으로 해당 사용자가 작성한 모든 댓글을 조회합니다.
     * 작성일(createdAt) 기준으로 내림차순 정렬하여 페이징 처리합니다.
     */
    Page<Comment> findByUser_LoginIdOrderByCreatedAtDesc(String loginId, Pageable pageable);
}