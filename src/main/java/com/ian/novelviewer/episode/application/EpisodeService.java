package com.ian.novelviewer.episode.application;

import com.ian.novelviewer.common.exception.CustomException;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.ian.novelviewer.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeService {

    private final NovelRepository novelRepository;
    private final EpisodeRepository episodeRepository;

    public Page<EpisodeDto.EpisodeTitleResponse> getAllEpisodes(Long contentId, Pageable pageable) {
        Novel novel = getNovel(contentId);

        Page<Episode> episodes = episodeRepository.findByNovel(novel, pageable);

        return episodes.map(EpisodeDto.EpisodeTitleResponse::from);
    }

    @Transactional
    public EpisodeDto.EpisodeInfoResponse createEpisode(
            Long contentId, EpisodeDto.CreateEpisodeRequest request, CustomUserDetails user
    ) {
        log.info("회차 등록 처리: {}", request.getTitle());

        Novel novel = getNovel(contentId);

        if (!novel.getAuthor().getLoginId().equals(user.getUsername())) {
            log.error("회차 등록 권한 없음 - 작가: {}, 작성자: {}", novel.getAuthor().getLoginId(), user.getUsername());
            throw new CustomException(NO_PERMISSION);
        }

        Episode episode = episodeRepository.save(
                Episode.builder()
                        .title(request.getTitle())
                        .content(request.getContent())
                        .novel(novel)
                        .build()
        );

        log.info("회차 등록 성공: {}", episode.getTitle());

        return EpisodeDto.EpisodeInfoResponse.from(episode);
    }

    public EpisodeDto.EpisodeContentResponse getEpisode(Long contentId, Long episodeId) {
        Novel novel = getNovel(contentId);

        Episode episode = getEpisode(episodeId);

        if (!episode.getNovel().getContentId().equals(novel.getContentId())) {
            log.error("회차가 해당 작품에 속하지 않음: episodeId={}, contentId={}", episodeId, contentId);
            throw new CustomException(EPISODE_NOT_FOUND);
        }

        return EpisodeDto.EpisodeContentResponse.from(episode);
    }

    private Novel getNovel(Long contentId) {
        Novel novel = novelRepository.findByContentId(contentId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 작품: {}", contentId);
                    return new CustomException(NOVEL_NOT_FOUND);
                });
        return novel;
    }

    @Transactional
    public EpisodeDto.EpisodeInfoResponse updateEpisode(
            Long contentId, Long episodeId,
            EpisodeDto.UpdateEpisodeRequest request, CustomUserDetails user
    ) {
        log.info("회차 수정 처리: {}", episodeId);
        Novel novel = getNovel(contentId);

        if (!novel.getAuthor().getLoginId().equals(user.getUsername())) {
            log.error("회차 수정 권한 없음 - 작가: {}, 요청자: {}", novel.getAuthor().getLoginId(), user.getUsername());
            throw new CustomException(NO_PERMISSION);
        }

        Episode episode = getEpisode(episodeId);

        if (StringUtils.hasText(request.getTitle())) {
            log.info("회차 제목 수정: {}", episode.getTitle());
            episode.changeTitle(request.getTitle());
        }

        if (StringUtils.hasText(request.getContent())) {
            log.info("회차 내용 수정: {}", request.getContent());
            episode.changeContent(request.getContent());
        }

        log.info("작품 수정 성공");
        return EpisodeDto.EpisodeInfoResponse.from(episode);
    }

    private Episode getEpisode(Long episodeId) {
        return episodeRepository.findById(episodeId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 회차: {}", episodeId);
                    return new CustomException(EPISODE_NOT_FOUND);
                });
    }
}
