package com.ian.novelviewer.novel.ui;

import com.ian.novelviewer.common.security.CustomUserDetails;
import com.ian.novelviewer.novel.application.NovelService;
import com.ian.novelviewer.novel.domain.Category;
import com.ian.novelviewer.novel.dto.NovelDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/novels")
@RequiredArgsConstructor
public class NovelController {

    private final NovelService novelService;


    /**
     * 모든 소설 목록을 조회합니다.
     *
     * @param category (선택) 카테고리 필터
     * @param page     페이지 번호 (기본값: 0)
     * @param size     페이지 크기 (기본값: 10)
     * @return 소설 목록 페이지
     */
    @GetMapping
    public ResponseEntity<?> getAllNovels(
            @RequestParam(name = "category", required = false) Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /novels - 작품 목록 조회 요청 (category={}, page={}, size={})", category, page, size);

        Pageable pageable = getPageable(page, size);
        Page<NovelDto.NovelResponse> responses = novelService.getAllNovels(category, pageable);

        log.info("GET /novels - 조회 완료 (총 {}건)", responses.getTotalElements());
        return ResponseEntity.ok(responses);
    }


    /**
     * 키워드를 이용하여 소설을 검색합니다.
     *
     * @param keyword 검색할 키워드
     * @param page    페이지 번호 (기본값: 0)
     * @param size    페이지 크기 (기본값: 10)
     * @return 검색 결과 소설 목록 페이지
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchNovel(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /novels/search - 검색 요청 (keyword='{}', page={}, size={})", keyword, page, size);

        Pageable pageable = getPageable(page, size);
        Page<NovelDto.NovelResponse> responses = novelService.searchNovel(keyword, pageable);

        log.info("GET /novels/search - 검색 완료 (총 {}건)", responses.getTotalElements());
        return ResponseEntity.ok(responses);
    }


    /**
     * 소설 섬네일 이미지를 업로드합니다.
     * 작가 권한이 있는 사용자만 업로드가 가능합니다.
     *
     * @param file 업로드할 섬네일 이미지 파일
     * @return 업로드된 섬네일 정보
     * @throws IOException 파일 업로드 중 예외 발생 가능
     */
    @PostMapping(
            value = "/thumbnails",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> uploadThumbnail(@RequestParam("thumbnail") MultipartFile file) throws IOException {
        log.info("POST /novels/thumbnails - 섬네일 업로드 요청 (파일명={})", file.getOriginalFilename());

        NovelDto.ThumbnailResponse response = novelService.uploadThumbnail(file);

        log.info("POST /novels/thumbnails - 업로드 완료 (thumbnailKey={})", response.getThumbnailKey());
        return ResponseEntity.ok(response);
    }


    /**
     * 새 소설을 등록합니다.
     * 작가 권한이 있는 사용자만 등록이 가능합니다.
     *
     * @param request 등록할 소설 요청 DTO
     * @param user    인증된 사용자 정보
     * @return 등록된 소설 정보
     */
    @PostMapping
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> createNovel(
            @RequestBody @Valid NovelDto.CreateNovelRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("POST /novels - 작품 등록 요청 by {} (제목='{}')", user.getUsername(), request.getTitle());

        NovelDto.NovelInfoResponse response = novelService.createNovel(request, user);

        log.info("POST /novels - 등록 완료 (novelId={}, 제목='{}')", response.getNovelId(), response.getTitle());
        return ResponseEntity.ok(response);
    }


    /**
     * 특정 소설의 상세 정보를 조회합니다.
     *
     * @param novelId 조회할 소설 고유번호
     * @return 소설 상세 정보
     */
    @GetMapping("/{novelId}")
    public ResponseEntity<?> getNovel(@PathVariable Long novelId) {
        log.info("GET /novels/{} - 작품 조회 요청", novelId);

        NovelDto.NovelInfoResponse novel = novelService.getNovel(novelId);

        log.info("GET /novels/{} - 조회 완료 (제목='{}')", novelId, novel.getTitle());
        return ResponseEntity.ok(novel);
    }


    /**
     * 소설 썸네일 이미지를 수정합니다.
     * 작가 권한이 있는 사용자만 수정할 수 있으며, 본인의 작품만 수정할 수 있습니다.
     *
     * @param novelId        섬네일을 수정할 소설 고유번호
     * @param file           새 섬네일 이미지 파일
     * @param authentication 인증 객체
     * @return 수정된 소설 정보
     * @throws IOException 파일 업로드 중 예외 발생 가능
     */
    @PutMapping(
            value = "/{novelId}/thumbnails",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> updateThumbnail(
            @PathVariable Long novelId,
            @RequestParam("thumbnail") MultipartFile file,
            Authentication authentication
    ) throws IOException {
        String loginId = getLoginId(authentication);
        log.info("PUT /novels/{}/thumbnails - 섬네일 수정 요청 by {} (파일명={})",
                novelId, loginId, file.getOriginalFilename());

        NovelDto.NovelInfoResponse response = novelService.updateThumbnail(novelId, file, loginId);

        log.info("PUT /novels/{}/thumbnails - 수정 완료 (thumbnailKey={})", novelId, response.getThumbnail());
        return ResponseEntity.ok(response);
    }


    /**
     * 기존 소설 정보를 수정합니다.
     * 작가 권한이 있는 사용자만 수정할 수 있으며, 본인의 작품만 수정할 수 있습니다.
     *
     * @param novelId        수정할 소설 고유번호
     * @param request        수정할 소설 요청 DTO
     * @param authentication 인증 객체
     * @return 수정된 소설 정보
     */
    @PatchMapping("/{novelId}")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> updateNovel(
            @PathVariable Long novelId,
            @RequestBody NovelDto.UpdateNovelRequest request,
            Authentication authentication
    ) {
        String loginId = getLoginId(authentication);
        log.info("PATCH /novels/{} - 작품 수정 요청 by {}", novelId, loginId);

        NovelDto.NovelInfoResponse response = novelService.updateNovel(novelId, request, loginId);

        log.info("PATCH /novels/{} - 수정 완료 (제목='{}')", novelId, response.getTitle());
        return ResponseEntity.ok(response);
    }


    /**
     * 소설을 삭제합니다.
     * 작가 권한이 있는 사용자의 경우, 본인의 작품만 삭제할 수 있습니다.
     * 관리자는 모든 작품을 삭제할 수 있습니다.
     *
     * @param novelId 삭제할 소설 고유번호
     * @param user    인증된 사용자 정보
     * @return 삭제 성공 메시지
     * @throws IOException 삭제 중 예외 발생 가능
     */
    @DeleteMapping("/{novelId}")
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public ResponseEntity<?> deleteNovel(
            @PathVariable Long novelId,
            @AuthenticationPrincipal CustomUserDetails user
    ) throws IOException {
        log.info("DELETE /novels/{} - 삭제 요청 by {}", novelId, user.getUsername());

        novelService.deleteNovel(novelId, user);

        log.info("DELETE /novels/{} - 삭제 완료", novelId);
        return ResponseEntity.ok("작품 삭제에 성공했습니다.");
    }


    /**
     * 인증 객체에서 현재 사용자의 로그인 ID를 추출합니다.
     *
     * @param authentication 인증 객체
     * @return 현재 사용자 로그인 ID
     */
    private static String getLoginId(Authentication authentication) {
        return authentication.getName();
    }


    /**
     * 페이지 요청 정보를 생성합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return Pageable 객체
     */
    private static Pageable getPageable(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return pageable;
    }


    /**
     * 소설을 선호 작품(북마크)에 추가합니다.
     *
     * @param novelId 북마크할 소설 고유번호
     * @param user    인증된 사용자 정보
     * @return 북마크 완료 메시지
     */
    @PostMapping("/{novelId}/bookmarks")
    public ResponseEntity<?> bookmark(
            @PathVariable Long novelId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("POST /novels/{}/bookmark - 북마크 요청 by {}", novelId, user.getUsername());

        novelService.bookmark(novelId, user.getUser().getId());

        log.info("POST /novels/{}/bookmark - 북마크 완료", novelId);
        return ResponseEntity.ok("작품을 선호 작품에 저장하였습니다.");
    }


    /**
     * 소설을 선호 작품(북마크)에서 제거합니다.
     *
     * @param novelId 북마크 해제할 소설 고유번호
     * @param user    인증된 사용자 정보
     * @return 북마크 해제 완료 메시지
     */
    @DeleteMapping("/{novelId}/bookmarks")
    public ResponseEntity<?> unbookmark(
            @PathVariable Long novelId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("DELETE /novels/{}/bookmark - 북마크 해제 요청 by {}", novelId, user.getUsername());

        novelService.unbookmark(novelId, user.getUser().getId());

        log.info("DELETE /novels/{}/bookmark - 북마크 해제 완료", novelId);
        return ResponseEntity.ok("작품을 선호 작품에서 삭제하였습니다.");
    }
}