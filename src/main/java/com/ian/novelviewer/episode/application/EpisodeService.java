package com.ian.novelviewer.episode.application;

import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.redis.RedisKeyUtil;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.ian.novelviewer.common.enums.Role.ROLE_ADMIN;
import static com.ian.novelviewer.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeService {

    private final NovelRepository novelRepository;
    private final EpisodeRepository episodeRepository;
    private final RedisTemplate<String, String> redisTemplate;


    /**
     * 특정 작품의 전체 회차 목록을 페이징하여 조회합니다.
     *
     * @param novelId 작품 고유번호
     * @param page    페이지 번호
     * @param size    페이지 크기
     * @return 에피소드 타이틀 응답의 Page 객체
     */
    public Page<EpisodeDto.EpisodeTitleResponse> getAllEpisodes(Long novelId, int page, int size) {
        log.debug("회차 목록 요청 - novelId={}, page={}, size={}", novelId, page, size);

        Pageable pageable = getPageable(page, size);
        Novel novel = findNovelOrThrow(novelId);

        Page<Episode> episodes = episodeRepository.findByNovel(novel, pageable);

        log.debug("회차 목록 조회 완료 - novelId={}, 총 회차 수={}", novelId, episodes.getTotalElements());
        return episodes.map(EpisodeDto.EpisodeTitleResponse::from);
    }

    private static PageRequest getPageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by("episodeId").ascending());
    }


    /**
     * 회차를 생성하고 저장합니다.
     *
     * @param novelId 작품 고유번호
     * @param request 회차 생성 요청 DTO
     * @param loginId 요청자 로그인 ID
     * @return 생성된 회차 정보 DTO
     */
    @Transactional
    public EpisodeDto.EpisodeInfoResponse createEpisode(
            Long novelId, EpisodeDto.CreateEpisodeRequest request, String loginId
    ) {
        log.debug("회차 등록 요청 - novelId={}, 요청자={}", novelId, loginId);
        log.debug("회차 제목={}, 내용(앞 30자)={}", request.getTitle(),
                request.getContent().substring(0, Math.min(30, request.getContent().length())));

        Novel novel = findNovelOrThrow(novelId);
        checkPermissionOrThrow(loginId, novel);

        Long episodeId = getNextEpisodeId(novel.getId());
        log.debug("회차 ID 생성 완료 - episodeId={}", episodeId);

        Episode episode = episodeRepository.save(
                Episode.builder()
                        .episodeId(episodeId)
                        .title(request.getTitle())
                        .content(request.getContent())
                        .novel(novel)
                        .build()
        );

        log.debug("회차 등록 완료 - episodeId={}", episode.getEpisodeId());
        return EpisodeDto.EpisodeInfoResponse.from(episode);
    }


    /**
     * 특정 회차를 조회합니다.
     *
     * @param novelId   작품 고유번호
     * @param episodeId 회차 고유번호
     * @return 회차 내용 응답 DTO
     */
    public EpisodeDto.EpisodeContentResponse getEpisode(Long novelId, Long episodeId) {
        log.debug("회차 단건 조회 요청 - novelId={}, episodeId={}", novelId, episodeId);

        Episode episode = findEpisodeOrThrow(novelId, episodeId);

        log.debug("회차 조회 성공 - episodeId={}", episode.getEpisodeId());
        return EpisodeDto.EpisodeContentResponse.from(episode);
    }


    /**
     * 특정 회차를 수정합니다.
     *
     * @param novelId   작품 고유번호
     * @param episodeId 회차 고유번호
     * @param request   수정 요청 DTO
     * @param loginId   요청자 로그인 ID
     * @return 수정된 회차 정보 DTO
     */
    @Transactional
    public EpisodeDto.EpisodeInfoResponse updateEpisode(
            Long novelId,
            Long episodeId,
            EpisodeDto.UpdateEpisodeRequest request,
            String loginId
    ) {
        log.debug("회차 수정 요청 - novelId={}, episodeId={}, 요청자={}", novelId, episodeId, loginId);

        Novel novel = findNovelOrThrow(novelId);
        Episode episode = findEpisodeOrThrow(novelId, episodeId);

        checkPermissionOrThrow(loginId, novel);

        if (StringUtils.hasText(request.getTitle())) {
            log.debug("회차 제목 수정 - 기존: {}, 변경: {}", episode.getTitle(), request.getTitle());
            episode.changeTitle(request.getTitle());
        }

        if (StringUtils.hasText(request.getContent())) {
            log.debug("회차 내용 수정 - 변경된 내용 길이: {}자", request.getContent().length());
            episode.changeContent(request.getContent());
        }

        log.debug("회차 수정 완료 - episodeId={}", episode.getEpisodeId());
        return EpisodeDto.EpisodeInfoResponse.from(episode);
    }


    /**
     * 특정 회차를 삭제합니다.
     * 요청자는 관리자이거나 해당 작품의 작가여야 합니다.
     *
     * @param novelId   작품 고유번호
     * @param episodeId 회차 고유번호
     * @param user      요청자 정보
     */
    @Transactional
    public void deleteEpisode(Long novelId, Long episodeId, CustomUserDetails user) {
        log.debug("회차 삭제 요청 - novelId={}, episodeId={}, 요청자={}", novelId, episodeId, user.getUsername());

        Novel novel = findNovelOrThrow(novelId);
        Episode episode = findEpisodeOrThrow(novelId, episodeId);

        boolean isAdmin = user.getUser().getRoles().contains(ROLE_ADMIN);
        boolean isAuthor = novel.getAuthor().getLoginId().equals(user.getUsername());
        log.debug("권한 확인 - isAdmin={}, isAuthor={}", isAdmin, isAuthor);

        if (!isAdmin && !isAuthor) {
            log.error("회차 삭제 권한 없음 - 소유자={}, 요청자={}", novel.getAuthor().getLoginId(), user.getUsername());
            throw new CustomException(NO_PERMISSION);
        }

        episodeRepository.delete(episode);
        log.debug("회차 삭제 완료 - episodeId={}", episodeId);
    }


    /**
     * 작품 고유번호에 해당하는 작품을 조회합니다.
     * 없을 경우 예외를 발생시킵니다.
     *
     * @param novelId 작품 고유번호
     * @return 조회된 작품 엔티티
     */
    private Novel findNovelOrThrow(Long novelId) {
        return novelRepository.findByNovelId(novelId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 작품: {}", novelId);
                    return new CustomException(NOVEL_NOT_FOUND);
                });
    }


    /**
     * 작품 고유번호 및 회차 고유번호로 회차를 조회합니다.
     * 없을 경우 예외를 발생시킵니다.
     *
     * @param novelId   작품 고유번호
     * @param episodeId 회차 고유번호
     * @return 조회된 회차 엔티티
     */
    private Episode findEpisodeOrThrow(Long novelId, Long episodeId) {
        return episodeRepository.findByEpisodeIdAndNovel_NovelId(episodeId, novelId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 회차: {}", episodeId);
                    return new CustomException(EPISODE_NOT_FOUND);
                });
    }


    /**
     * 작품의 작성자가 현재 사용자와 일치하는지 확인합니다.
     * 일치하지 않을 경우 예외를 발생시킵니다.
     *
     * @param loginId 현재 사용자 로그인 ID
     * @param novel   대상 작품
     */
    private static void checkPermissionOrThrow(String loginId, Novel novel) {
        if (!novel.getAuthor().getLoginId().equals(loginId)) {
            log.error("회차 등록 권한 없음 - 작가: {}, 작성자: {}", novel.getAuthor().getLoginId(), loginId);
            throw new CustomException(NO_PERMISSION);
        }
    }


    /**
     * Redis를 사용해 작품별로 다음 회차 고유번호를 생성합니다.
     *
     * @param novelId 작품 고유번호
     * @return 다음 회차 고유번호
     */
    public Long getNextEpisodeId(Long novelId) {
        String key = RedisKeyUtil.episodeIdKey(novelId);
        Long nextId = redisTemplate.opsForValue().increment(key);
        log.debug("다음 회차 ID 생성 - novelId={}, nextEpisodeId={}", novelId, nextId);
        return nextId;
    }
}