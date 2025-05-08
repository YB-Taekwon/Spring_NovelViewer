package com.ian.novelviewer.episode.application;

import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.exception.ErrorCode;
import com.ian.novelviewer.common.security.CustomUserDetails;
import com.ian.novelviewer.episode.domain.Episode;
import com.ian.novelviewer.episode.domain.EpisodeRepository;
import com.ian.novelviewer.episode.dto.EpisodeDto;
import com.ian.novelviewer.novel.domain.Novel;
import com.ian.novelviewer.novel.domain.NovelRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.ian.novelviewer.common.exception.ErrorCode.NOVEL_NOT_FOUND;
import static com.ian.novelviewer.common.exception.ErrorCode.NO_PERMISSION;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeService {

    private final NovelRepository novelRepository;
    private final EpisodeRepository episodeRepository;

    @Transactional
    public EpisodeDto.EpisodeInfoResponse createEpisode(
            Long contentId, EpisodeDto.CreateEpisodeRequest request, CustomUserDetails user
    ) {
        log.info("회차 등록 처리: {}", request.getTitle());

        Novel novel = novelRepository.findByContentId(contentId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 작품: {}", contentId);
                    return new CustomException(NOVEL_NOT_FOUND);
                });

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
}
