package com.ian.novelviewer.episode.domain;

import com.ian.novelviewer.novel.domain.Novel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, Long> {
    /**
     * 주어진 소설에 해당하는 에피소드들을 페이징 처리하여 반환
     */
    Page<Episode> findByNovel(Novel novel, Pageable pageable);

    /**
     * 주어진 소설 ID와 에피소드 ID에 해당하는 에피소드를 조회
     */
    Optional<Episode> findByEpisodeIdAndNovel_NovelId(Long NovelId, Long episodeId);
}