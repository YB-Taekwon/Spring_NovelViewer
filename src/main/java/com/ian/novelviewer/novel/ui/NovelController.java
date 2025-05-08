package com.ian.novelviewer.novel.ui;

import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.security.CustomUserDetails;
import com.ian.novelviewer.novel.application.NovelService;
import com.ian.novelviewer.novel.application.S3Service;
import com.ian.novelviewer.novel.domain.Category;
import com.ian.novelviewer.novel.dto.NovelDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import static com.ian.novelviewer.common.exception.ErrorCode.*;

@RestController
@RequestMapping("/novels")
@RequiredArgsConstructor
public class NovelController {

    private final NovelService novelService;
    private final S3Service s3Service;

    @GetMapping
    public ResponseEntity<?> getAllNovels(
            @RequestParam(name = "category", required = false) Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NovelDto.NovelResponse> responses = novelService.getAllNovels(category, pageable);

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchNovel(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NovelDto.NovelResponse> responses = novelService.searchNovel(keyword, pageable);

        return ResponseEntity.ok(responses);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> createNovel(
            @RequestPart("novel") @Valid NovelDto.CreateNovelRequest request,
            @RequestPart("thumbnail") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        String imageKey = null;
        try {
            imageKey = s3Service.upload(file, "thumbnails");

            NovelDto.NovelInfoResponse response = novelService.createNovel(request, imageKey, user);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            throw new CustomException(S3_UPLOAD_FAILED);
        } catch (Exception e) {
            if (imageKey != null) {
                try {
                    s3Service.delete(imageKey);
                } catch (Exception ex) {
                    throw new CustomException(S3_ROLLBACK_FAILED);
                }
            }
            throw new CustomException(NOVEL_CREATION_FAILED);
        }
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<?> getNovel(@PathVariable Long contentId) {
        NovelDto.NovelInfoResponse novel = novelService.getNovel(contentId);

        return ResponseEntity.ok(novel);
    }
}