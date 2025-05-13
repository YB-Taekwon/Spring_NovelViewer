package com.ian.novelviewer.episode.ui;

import com.ian.novelviewer.common.security.CustomUserDetails;
import com.ian.novelviewer.episode.application.EpisodeService;
import com.ian.novelviewer.episode.dto.EpisodeDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/novels/{novelId}/episodes")
@RequiredArgsConstructor
public class EpisodeController {

    private final EpisodeService episodeService;

    @GetMapping
    public ResponseEntity<?> getAllEpisodes(
            @PathVariable Long novelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("모든 회차 조회 요청: {}", novelId);
        Page<EpisodeDto.EpisodeTitleResponse> responses =
                episodeService.getAllEpisodes(novelId, page, size);

        log.info("모든 회차 조회 완료");
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> createEpisode(
            @PathVariable Long novelId,
            @RequestBody @Valid EpisodeDto.CreateEpisodeRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("회차 등록 요청: {}", request.getTitle());
        EpisodeDto.EpisodeInfoResponse response = episodeService.createEpisode(novelId, request, user);

        log.info("회차 등록 완료: {}", response.getTitle());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{episodeId}")
    public ResponseEntity<?> getEpisode(
            @PathVariable Long novelId, @PathVariable Long episodeId
    ) {
        log.info("회차 조회 요청: {}", novelId);
        EpisodeDto.EpisodeContentResponse response = episodeService.getEpisode(novelId, episodeId);

        log.info("회차 조회 완료");
        return ResponseEntity.ok(response);
    }
}