package com.ian.novelviewer.episode.ui;

import com.ian.novelviewer.common.security.CustomUserDetails;
import com.ian.novelviewer.episode.application.EpisodeService;
import com.ian.novelviewer.episode.dto.EpisodeDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
        log.info("GET /novels/{}/episodes - 회차 목록 요청 (page={}, size={})", novelId, page, size);

        Page<EpisodeDto.EpisodeTitleResponse> responses =
                episodeService.getAllEpisodes(novelId, page, size);

        log.info("GET /novels/{}/episodes - 회차 목록 응답 완료 (총 {}건)",
                novelId, responses.getTotalElements());
        return ResponseEntity.ok(responses);
    }


    @PostMapping
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> createEpisode(
            @PathVariable Long novelId,
            @RequestBody @Valid EpisodeDto.CreateEpisodeRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("POST /novels/{}/episodes - 회차 등록 요청 by {} - 제목: {}",
                novelId, user.getUsername(), request.getTitle());
        EpisodeDto.EpisodeInfoResponse response = episodeService.createEpisode(novelId, request, user);

        log.info("POST /novels/{}/episodes - 회차 등록 완료 - episodeId: {}, 제목: {}",
                novelId, response.getEpisodeId(), response.getTitle());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{episodeId}")
    public ResponseEntity<?> getEpisode(
            @PathVariable Long novelId, @PathVariable Long episodeId
    ) {
        log.info("GET /novels/{}/episodes/{} - 회차 조회 요청", novelId, episodeId);

        EpisodeDto.EpisodeContentResponse response = episodeService.getEpisode(novelId, episodeId);

        log.info("GET /novels/{}/episodes/{} - 회차 조회 완료", novelId, episodeId);
        return ResponseEntity.ok(response);
    }


    @PatchMapping("/{episodeId}")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> updateEpisode(
            @PathVariable Long novelId,
            @PathVariable Long episodeId,
            @RequestBody EpisodeDto.UpdateEpisodeRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("PATCH /novels/{}/episodes/{} - 회차 수정 요청 by {}",
                novelId, episodeId, user.getUsername());

        EpisodeDto.EpisodeInfoResponse response =
                episodeService.updateEpisode(novelId, episodeId, request, user);

        log.info("PATCH /novels/{}/episodes/{} - 회차 수정 완료", novelId, episodeId);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{episodeId}")
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public ResponseEntity<?> deleteEpisode(
            @PathVariable Long novelId,
            @PathVariable Long episodeId,
            @AuthenticationPrincipal CustomUserDetails user

    ) {
        log.info("DELETE /novels/{}/episodes/{} - 회차 삭제 요청 by {}",
                novelId, episodeId, user.getUsername());

        episodeService.deleteEpisode(novelId, episodeId, user);

        log.info("DELETE /novels/{}/episodes/{} - 회차 삭제 완료", novelId, episodeId);
        return ResponseEntity.ok("회차 삭제가 완료되었습니다.");
    }
}