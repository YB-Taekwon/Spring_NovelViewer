package com.ian.novelviewer.episode.application;

import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.redis.EpisodeIdService;
import com.ian.novelviewer.common.security.CustomUserDetails;
import com.ian.novelviewer.episode.domain.Episode;
import com.ian.novelviewer.episode.domain.EpisodeRepository;
import com.ian.novelviewer.episode.dto.EpisodeDto;
import com.ian.novelviewer.novel.domain.Novel;
import com.ian.novelviewer.novel.domain.NovelRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import static com.ian.novelviewer.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeService {

    private final NovelRepository novelRepository;
    private final EpisodeRepository episodeRepository;
    private final EpisodeIdService episodeIdService;

    public Page<EpisodeDto.EpisodeTitleResponse> getAllEpisodes(Long novelId, int page, int size) {
        log.info("모든 회차 조회 처리: {}", novelId);
        Pageable pageable = getPageable(page, size);

        Novel novel = findNovelOrThrow(novelId);

        Page<Episode> episodes = episodeRepository.findByNovel(novel, pageable);

        log.info("모든 회차 조회 성공");
        return episodes.map(EpisodeDto.EpisodeTitleResponse::from);
    }

    private static PageRequest getPageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by("episodeId").ascending());
    }


    @Transactional
    public EpisodeDto.EpisodeInfoResponse createEpisode(
            Long novelId, EpisodeDto.CreateEpisodeRequest request, CustomUserDetails user
    ) {
        log.info("회차 등록 처리: {}", request.getTitle());
        Novel novel = findNovelOrThrow(novelId);

        checkPermissionOrThrow(user, novel);

        Long episodeId = episodeIdService.getNextEpisodeId(novel.getId());

        Episode episode = episodeRepository.save(
                Episode.builder()
                        .episodeId(episodeId)
                        .title(request.getTitle())
                        .content(request.getContent())
                        .novel(novel)
                        .build()
        );

        log.info("회차 등록 성공: {}", episode.getTitle());
        return EpisodeDto.EpisodeInfoResponse.from(episode);
    }


    public EpisodeDto.EpisodeContentResponse getEpisode(Long novelId, Long episodeId) {
        log.info("회차 조회 처리: {}", novelId);
        Novel novel = findNovelOrThrow(novelId);

        Episode episode = findEpisodeOrThrow(episodeId);

        checkEpisodeBelongsToNovelOrThrow(novelId, episodeId, episode, novel);

        log.info("회차 조회 성공: {}", episode.getTitle());
        return EpisodeDto.EpisodeContentResponse.from(episode);
    }


    private Novel findNovelOrThrow(Long novelId) {
        return novelRepository.findByNovelId(novelId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 작품: {}", novelId);
                    return new CustomException(NOVEL_NOT_FOUND);
                });
    }

    private Episode findEpisodeOrThrow(Long episodeId) {
        return episodeRepository.findById(episodeId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 회차: {}", episodeId);
                    return new CustomException(EPISODE_NOT_FOUND);
                });
    }

    private static void checkPermissionOrThrow(CustomUserDetails user, Novel novel) {
        if (!novel.getAuthor().getLoginId().equals(user.getUsername())) {
            log.error("회차 등록 권한 없음 - 작가: {}, 작성자: {}", novel.getAuthor().getLoginId(), user.getUsername());
            throw new CustomException(NO_PERMISSION);
        }
    }

    private static void checkEpisodeBelongsToNovelOrThrow(
            Long novelId, Long episodeId, Episode episode, Novel novel
    ) {
        if (!episode.getNovel().getNovelId().equals(novel.getNovelId())) {
            log.error("회차가 해당 작품에 속하지 않음: episodeId={}, novelId={}", episodeId, novelId);
            throw new CustomException(EPISODE_NOT_FOUND);
        }
    }
}