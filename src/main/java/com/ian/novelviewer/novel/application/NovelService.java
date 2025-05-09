package com.ian.novelviewer.novel.application;

import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.security.CustomUserDetails;
import com.ian.novelviewer.novel.domain.Category;
import com.ian.novelviewer.novel.domain.Novel;
import com.ian.novelviewer.novel.domain.NovelRepository;
import com.ian.novelviewer.novel.dto.NovelDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

import static com.ian.novelviewer.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelService {

    private final NovelRepository novelRepository;
    private final S3Service s3Service;

    private static final String S3_FOLDER_NAME = "thumbnails";

    public Page<NovelDto.NovelResponse> getAllNovels(Category category, Pageable pageable) {
        Page<Novel> novels;

        if (category == null) {
            novels = novelRepository.findAll(pageable);
        } else {
            log.info("필터링 요청 처리: {}", category);
            novels = novelRepository.findAllByCategory(category, pageable);
        }

        return novels.map(NovelDto.NovelResponse::from);
    }

    public Page<NovelDto.NovelResponse> searchNovel(String keyword, Pageable pageable) {
        if (!StringUtils.hasText(keyword)) {
            log.error("검색어가 입력되지 않음: {}", keyword);
            throw new CustomException(INVALID_KEYWORD);
        }

        Page<Novel> novels = novelRepository.findByTitleOrAuthorName(keyword, pageable);

        return novels.map(NovelDto.NovelResponse::from);
    }

    @Transactional
    public NovelDto.ThumbnailResponse uploadThumbnail(MultipartFile file) throws IOException {
        log.info("섬네일 등록 처리: {}", file.getOriginalFilename());

        validateFile(file);

        String key = s3Service.upload(file, S3_FOLDER_NAME);

        log.info("섬네일 등록 성공");
        return NovelDto.ThumbnailResponse.builder().thumbnailKey(key).build();
    }

    @Transactional
    public NovelDto.NovelInfoResponse createNovel(
            NovelDto.CreateNovelRequest request, CustomUserDetails user
    ) {
        log.info("작품 등록 처리: {}", request.getTitle());

        Long novelId = generateNovelId();
        log.info("작품 고유 번호 생성: {}", novelId);

        Novel novel = novelRepository.save(
                Novel.builder()
                        .novelId(novelId)
                        .title(request.getTitle())
                        .thumbnail(request.getThumbnail())
                        .description(request.getDescription())
                        .category(request.getCategory())
                        .author(user.getUser())
                        .build()
        );

        log.info("작품 등록 성공");
        return NovelDto.NovelInfoResponse.from(novel);
    }

    public NovelDto.NovelInfoResponse getNovel(Long novelId) {
        log.info("작품 조회 처리: {}", novelId);

        Novel novel = novelRepository.findByNovelId(novelId)
                .orElseThrow(() -> {
                    log.error("작품을 찾을 수 없습니다. {}", novelId);
                    return new CustomException(NOVEL_NOT_FOUND);
                });

        log.info("작품 조회 성공: {}", novel.getTitle());
        return NovelDto.NovelInfoResponse.from(novel);
    }

    private Long generateNovelId() {
        UUID uuid = UUID.randomUUID();

        return Math.abs(uuid.getMostSignificantBits());
    }

    @Transactional
    public NovelDto.NovelInfoResponse updateThumbnail(
            Long novelId, MultipartFile file, CustomUserDetails user
    ) throws IOException {
        log.info("섬네일 수정 처리: {}", novelId);
        Novel novel = findNovelOrThrow(novelId);

        checkPermissionOrThrow(novel, user);

        String oldKey = novel.getThumbnail();
        String newKey = s3Service.update(file, S3_FOLDER_NAME, oldKey);

        novel.changeThumbnail(newKey);

        log.info("섬네일 수정 성공");
        return NovelDto.NovelInfoResponse.from(novel);
    }

    @Transactional
    public NovelDto.NovelInfoResponse updateNovel(
            Long novelId, NovelDto.UpdateNovelRequest request, CustomUserDetails user
    ) {
        log.info("작품 수정 처리: {}", request.getTitle());
        Novel novel = findNovelOrThrow(novelId);

        checkPermissionOrThrow(novel, user);

        if (StringUtils.hasText(request.getTitle())) {
            log.info("작품명 변경: {}", request.getTitle());
            novel.changeTitle(request.getTitle());
        }

        if (StringUtils.hasText(request.getDescription())) {
            log.info("작품 소개 변경: {}", request.getDescription());
            novel.changeDescription(request.getDescription());
        }

        if (request.getCategory() != null) {
            log.info("작품 카테고리 변경: {}", request.getCategory());
            novel.changeCategory(request.getCategory());
        }

        log.info("작품 수정 성공");
        return NovelDto.NovelInfoResponse.from(novel);
    }

    @Transactional
    public void deleteNovel(Long novelId, CustomUserDetails user) throws IOException {
        log.info("작품 삭제 처리: {}", novelId);
        Novel novel = findNovelOrThrow(novelId);

        checkPermissionOrThrow(novel, user);

        novelRepository.delete(novel);
        s3Service.delete(novel.getThumbnail());

        log.info("작품 삭제 성공");
    }

    private Novel findNovelOrThrow(Long novelId) {
        return novelRepository.findByNovelId(novelId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 작품: {}", novelId);
                    return new CustomException(NOVEL_NOT_FOUND);
                });
    }

    private static void checkPermissionOrThrow(Novel novel, CustomUserDetails user) {
        if (!novel.getAuthor().getLoginId().equals(user.getUsername())) {
            log.error("해당 작업 권한 없음 - 작가: {}, 요청자: {}",
                    novel.getAuthor().getLoginId(), user.getUsername());
            throw new CustomException(NO_PERMISSION);
        }
    }

    private static void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            log.error("섬네일이 비어있음");
            throw new CustomException(FILE_EMPTY);
        }

        if (!file.getContentType().startsWith("image/")) {
            log.error("섬네일 파일의 형식이 이미지가 아님");
            throw new CustomException(INVALID_FILE_FORMAT);
        }
    }
}