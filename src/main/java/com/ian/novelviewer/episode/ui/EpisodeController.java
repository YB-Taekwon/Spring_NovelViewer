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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/novels/{novelId}/episodes")
@RequiredArgsConstructor
public class EpisodeController {

    private final EpisodeService episodeService;


    /**
     * 주어진 소설 고유번호에 해당하는 모든 회차 목록을 페이징하여 조회합니다.
     *
     * @param novelId 소설 고유번호
     * @param page    페이지 번호 (기본값: 0)
     * @param size    페이지당 항목 수 (기본값: 20)
     * @return 회차 제목 응답 페이지
     */
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


    /**
     * 소설에 새로운 회차를 등록합니다.
     * 작가 권한이 있는 사용자만 등록이 가능합니다.
     *
     * @param novelId        소설 고유번호
     * @param request        회차 생성 요청 DTO
     * @param authentication 인증 객체 (작성자 정보 추출)
     * @return 생성된 회차 정보 응답 DTO
     */
    @PostMapping
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> createEpisode(
            @PathVariable Long novelId,
            @RequestBody @Valid EpisodeDto.CreateEpisodeRequest request,
            Authentication authentication
    ) {
        String loginId = getLoginId(authentication);
        log.info("POST /novels/{}/episodes - 회차 등록 요청 by {} - 제목: {}",
                novelId, loginId, request.getTitle());

        EpisodeDto.EpisodeInfoResponse response = episodeService.createEpisode(novelId, request, loginId);

        log.info("POST /novels/{}/episodes - 회차 등록 완료 - episodeId: {}, 제목: {}",
                novelId, response.getEpisodeId(), response.getTitle());
        return ResponseEntity.ok(response);
    }


    /**
     * 특정 회차의 전체 내용을 조회합니다.
     *
     * @param novelId   소설 고유번호
     * @param episodeId 회차 고유번호
     * @return 회차 본문 응답 DTO
     */
    @GetMapping("/{episodeId}")
    public ResponseEntity<?> getEpisode(
            @PathVariable Long novelId, @PathVariable Long episodeId
    ) {
        log.info("GET /novels/{}/episodes/{} - 회차 조회 요청", novelId, episodeId);

        EpisodeDto.EpisodeContentResponse response = episodeService.getEpisode(novelId, episodeId);

        log.info("GET /novels/{}/episodes/{} - 회차 조회 완료", novelId, episodeId);
        return ResponseEntity.ok(response);
    }


    /**
     * 특정 회차의 정보를 수정합니다.
     * 작가 권한이 있는 사용자만 수정이 가능하며, 본인의 작품 및 회차만 수정이 가능합니다.
     *
     * @param novelId        소설 고유번호
     * @param episodeId      회차 고유번호
     * @param request        회차 수정 요청 DTO
     * @param authentication 인증 객체 (작성자 정보 확인)
     * @return 수정된 회차 정보 응답 DTO
     */
    @PatchMapping("/{episodeId}")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> updateEpisode(
            @PathVariable Long novelId,
            @PathVariable Long episodeId,
            @RequestBody EpisodeDto.UpdateEpisodeRequest request,
            Authentication authentication
    ) {
        String loginId = getLoginId(authentication);
        log.info("PATCH /novels/{}/episodes/{} - 회차 수정 요청 by {}",
                novelId, episodeId, loginId);

        EpisodeDto.EpisodeInfoResponse response =
                episodeService.updateEpisode(novelId, episodeId, request, loginId);

        log.info("PATCH /novels/{}/episodes/{} - 회차 수정 완료", novelId, episodeId);
        return ResponseEntity.ok(response);
    }


    /**
     * 특정 회차를 삭제합니다.
     * 작가 권한이 있는 경우, 본인의 작품 및 회차만 삭제가 가능합니다.
     * 관리자는 모든 회차를 삭제할 수 있습니다.
     *
     * @param novelId   소설 고유번호
     * @param episodeId 회차 고유번호
     * @param user      인증된 사용자 정보
     * @return 삭제 성공 메시지
     */
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


    /**
     * 인증 객체에서 로그인 ID를 추출합니다.
     *
     * @param authentication 인증 객체
     * @return 로그인 ID
     */
    private static String getLoginId(Authentication authentication) {
        return authentication.getName();
    }
}