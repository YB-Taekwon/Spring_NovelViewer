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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/novels/{contentId}/episodes")
@RequiredArgsConstructor
public class EpisodeController {

    private final EpisodeService episodeService;

    @GetMapping
    public ResponseEntity<?> getAllEpisodes(
            @PathVariable Long contentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<EpisodeDto.EpisodeTitleResponse> responses = episodeService.getAllEpisodes(contentId, pageable);

        return ResponseEntity.ok(responses);
    }

    @PostMapping
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> createEpisode(
            @PathVariable Long contentId,
            @RequestBody @Valid EpisodeDto.CreateEpisodeRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("회차 등록 요청: {}", request.getTitle());
        EpisodeDto.EpisodeInfoResponse response = episodeService.createEpisode(contentId, request, user);

        log.info("등록된 회차 반환: {}", response.getTitle());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{episodeId}")
    public ResponseEntity<?> getEpisode(
            @PathVariable Long contentId, @PathVariable Long episodeId
    ) {
        EpisodeDto.EpisodeContentResponse response = episodeService.getEpisode(contentId, episodeId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{episodeId}")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> updateEpisode(
            @PathVariable Long contentId,
            @PathVariable Long episodeId,
            @RequestBody EpisodeDto.UpdateEpisodeRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("회차 수정 요청: {}", episodeId);
        EpisodeDto.EpisodeInfoResponse response = episodeService.updateEpisode(contentId, episodeId, request, user);

        return ResponseEntity.ok(response);
    }
}
