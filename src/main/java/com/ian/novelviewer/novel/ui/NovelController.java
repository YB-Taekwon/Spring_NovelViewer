package com.ian.novelviewer.novel.ui;

import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.security.CustomUserDetails;
import com.ian.novelviewer.novel.application.NovelService;
import com.ian.novelviewer.novel.application.S3Service;
import com.ian.novelviewer.novel.dto.NovelDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static com.ian.novelviewer.common.exception.ErrorCode.*;

@Slf4j
@RestController
@RequestMapping("/novels")
@RequiredArgsConstructor
public class NovelController {

    private final NovelService novelService;
    private final S3Service s3Service;

    private static final String S3_FOLDER_NAME = "thumbnails";

    @PostMapping(
            value = "/thumbnails",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> uploadThumbnail(
            @RequestPart("thumbnail") MultipartFile file
    ) {
        try {
            log.info("섬네일 업로드 요청: {}", file.getOriginalFilename());
            NovelDto.ThumbnailResponse response = s3Service.upload(file, S3_FOLDER_NAME);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("섬네일 업로드 중 예외 발생: {}", e.getMessage());
            throw new CustomException(S3_UPLOAD_FAILED);
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<?> createNovel(
            @RequestBody @Valid NovelDto.CreateNovelRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("작품 등록 요청: {}", request.getTitle());
        NovelDto.NovelInfoResponse response = novelService.createNovel(request, user);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<?> getNovel(@PathVariable Long contentId) {
        log.info("작품 조회 요청: {}", contentId);
        NovelDto.NovelInfoResponse novel = novelService.getNovel(contentId);

        return ResponseEntity.ok(novel);
    }
}