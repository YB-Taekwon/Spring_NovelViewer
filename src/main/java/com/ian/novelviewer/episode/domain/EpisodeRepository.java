package com.ian.novelviewer.episode.domain;

import com.ian.novelviewer.novel.domain.Novel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, Long> {
    Page<Episode> findByNovel(Novel novel, Pageable pageable);

    Optional<Episode> findByNovel_NovelIdAndEpisodeId(Long NovelId, Long episodeId);
}