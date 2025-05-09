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

    @GetMapping
    public ResponseEntity<?> getAllNovels(
            @RequestParam(name = "category", required = false) Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("전체 작품 목록 조회 요청");

        Pageable pageable = getPageable(page, size);
        Page<NovelDto.NovelResponse> responses = novelService.getAllNovels(category, pageable);

        log.info("전체 작품 목록 조회 완료");
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchNovel(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("검색 요청 - 키워드: {}", keyword);

        Pageable pageable = getPageable(page, size);
        Page<NovelDto.NovelResponse> responses = novelService.searchNovel(keyword, pageable);

        log.info("검색 완료");
        return ResponseEntity.ok(responses);
    }

    @PostMapping(
            value = "/thumbnails",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> uploadThumbnail(@RequestParam("thumbnail") MultipartFile file) throws IOException {
        log.info("섬네일 업로드 요청: {}", file.getOriginalFilename());
        NovelDto.ThumbnailResponse response = novelService.uploadThumbnail(file);

        log.info("섬네일 업로드 완료: {}", response.getThumbnailKey());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> createNovel(
            @RequestBody @Valid NovelDto.CreateNovelRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("작품 등록 요청: {}", request.getTitle());
        NovelDto.NovelInfoResponse response = novelService.createNovel(request, user);

        log.info("작품 등록 완료: {}", response.getTitle());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{novelId}")
    public ResponseEntity<?> getNovel(@PathVariable Long novelId) {
        log.info("작품 조회 요청: {}", novelId);
        NovelDto.NovelInfoResponse novel = novelService.getNovel(novelId);

        log.info("작품 조회 완료: {}", novel.getTitle());
        return ResponseEntity.ok(novel);
    }

    @PutMapping(
            value = "/{novelId}/thumbnails",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> updateThumbnail(
            @PathVariable Long novelId,
            @RequestParam("thumbnail") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails user
    ) throws IOException {
        log.info("섬네일 수정 요청: {}", file.getOriginalFilename());
        NovelDto.NovelInfoResponse response = novelService.updateThumbnail(novelId, file, user);

        log.info("섬네일 수정 완료: {}", response.getThumbnail());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{novelId}")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> updateNovel(
            @PathVariable Long novelId,
            @RequestBody NovelDto.UpdateNovelRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("작품 수정 요청: {}", novelId);
        NovelDto.NovelInfoResponse response = novelService.updateNovel(novelId, request, user);

        log.info("작품 수정 완료: {}", response.getTitle());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{novelId}")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> deleteNovel(
            @PathVariable Long novelId,
            @AuthenticationPrincipal CustomUserDetails user
    ) throws IOException {
        log.info("작품 삭제 요청: {}", novelId);
        novelService.deleteNovel(novelId, user);

        log.info("작품 삭제 성공");
        return ResponseEntity.ok("작품 삭제에 성공했습니다.");
    }

    private static Pageable getPageable(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return pageable;
    }
}