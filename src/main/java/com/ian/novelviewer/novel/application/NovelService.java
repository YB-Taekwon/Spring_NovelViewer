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
        log.debug("작품 목록 조회 요청 - category={}, pageable={}", category, pageable);
        Page<Novel> novels;

        if (category == null) {
            log.debug("카테고리 필터 없음 - 전체 작품 조회 실행");
            novels = novelRepository.findAll(pageable);
        } else {
            log.debug("카테고리 필터 적용 - category={}", category);
            novels = novelRepository.findAllByCategory(category, pageable);
        }

        log.debug("조회된 작품 수: {}", novels.getTotalElements());
        return novels.map(NovelDto.NovelResponse::from);
    }


    public Page<NovelDto.NovelResponse> searchNovel(String keyword, Pageable pageable) {
        log.debug("작품 검색 요청 - keyword='{}', pageable={}", keyword, pageable);

        if (!StringUtils.hasText(keyword)) {
            log.error("입력된 검색 키워드가 없음");
            throw new CustomException(INVALID_KEYWORD);
        }

        Page<Novel> novels = novelRepository.findByTitleOrAuthorName(keyword, pageable);

        log.debug("검색 결과 작품 수: {}", novels.getTotalElements());
        return novels.map(NovelDto.NovelResponse::from);
    }


    @Transactional
    public NovelDto.ThumbnailResponse uploadThumbnail(MultipartFile file) throws IOException {
        log.debug("섬네일 업로드 요청 - 원본 파일명: {}", file.getOriginalFilename());

        validateFile(file);

        String key = s3Service.upload(file, S3_FOLDER_NAME);

        log.debug("섬네일 업로드 완료 - 저장된 key: {}", key);
        return NovelDto.ThumbnailResponse.builder().thumbnailKey(key).build();
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


    @Transactional
    public NovelDto.NovelInfoResponse createNovel(
            NovelDto.CreateNovelRequest request, CustomUserDetails user
    ) {
        log.debug("작품 등록 요청 - 제목: {}, 작성자: {}", request.getTitle(), user.getUsername());

        Long novelId = generateNovelId();
        log.debug("생성된 작품 고유 번호: {}", novelId);

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

        log.debug("작품 등록 완료 - novelId: {}", novel.getNovelId());
        return NovelDto.NovelInfoResponse.from(novel);
    }

    private Long generateNovelId() {
        return Math.abs(UUID.randomUUID().getMostSignificantBits());
    }


    public NovelDto.NovelInfoResponse getNovel(Long novelId) {
        log.debug("작품 단건 조회 요청 - novelId: {}", novelId);

        Novel novel = findNovelOrThrow(novelId);

        log.debug("조회된 작품 - 제목: {}", novel.getTitle());
        return NovelDto.NovelInfoResponse.from(novel);
    }


    @Transactional
    public NovelDto.NovelInfoResponse updateThumbnail(
            Long novelId, MultipartFile file, CustomUserDetails user
    ) throws IOException {
        log.debug("섬네일 수정 요청 - novelId={}, 파일명={}, 요청자={}", novelId, file.getOriginalFilename(), user.getUsername());

        Novel novel = findNovelOrThrow(novelId);
        checkPermissionOrThrow(novel, user);

        String oldKey = novel.getThumbnail();
        String newKey = s3Service.update(file, S3_FOLDER_NAME, oldKey);

        log.debug("섬네일 변경 완료 - oldKey={}, newKey={}", oldKey, newKey);
        novel.changeThumbnail(newKey);

        return NovelDto.NovelInfoResponse.from(novel);
    }


    @Transactional
    public NovelDto.NovelInfoResponse updateNovel(
            Long novelId, NovelDto.UpdateNovelRequest request, CustomUserDetails user
    ) {
        log.debug("작품 수정 요청 - novelId={}, 요청자={}", novelId, user.getUsername());

        Novel novel = findNovelOrThrow(novelId);
        checkPermissionOrThrow(novel, user);

        if (StringUtils.hasText(request.getTitle())) {
            log.debug("제목 변경: {}", request.getTitle());
            novel.changeTitle(request.getTitle());
        }

        if (StringUtils.hasText(request.getDescription())) {
            log.debug("설명 변경 있음");
            novel.changeDescription(request.getDescription());
        }

        if (request.getCategory() != null) {
            log.debug("카테고리 변경: {}", request.getCategory());
            novel.changeCategory(request.getCategory());
        }

        return NovelDto.NovelInfoResponse.from(novel);
    }


    @Transactional
    public void deleteNovel(Long novelId, CustomUserDetails user) throws IOException {
        log.debug("작품 삭제 요청 - novelId={}, 요청자={}", novelId, user.getUsername());

        Novel novel = findNovelOrThrow(novelId);
        checkPermissionOrThrow(novel, user);

        novelRepository.delete(novel);
        s3Service.delete(novel.getThumbnail());

        log.debug("작품 삭제 완료 - novelId={}", novelId);
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
}