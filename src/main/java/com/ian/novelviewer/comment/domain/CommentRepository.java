package com.ian.novelviewer.comment.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findAllByEpisode_Novel_NovelIdAndEpisode_EpisodeIdAndParentIsNull
            (Long novelId, Long episodeId, Pageable pageable);

    Optional<Comment> findByIdAndEpisode_EpisodeIdAndEpisode_Novel_NovelId(Long novelId, Long episodeId, Long commentId);
}