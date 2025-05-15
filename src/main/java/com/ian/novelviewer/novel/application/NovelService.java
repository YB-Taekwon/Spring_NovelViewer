package com.ian.novelviewer.novel.application;

import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.redis.RedisKeyUtil;
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
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String, String> redisTemplate;

    private static final String S3_FOLDER_NAME = "thumbnails";


    /**
     * 모든 소설을 카테고리 조건에 따라 조회합니다.
     *
     * @param category 소설 카테고리 (null이면 전체 조회)
     * @param pageable 페이징 정보
     * @return 소설 목록 페이지
     */
    public Page<NovelDto.NovelResponse> getAllNovels(Category category, Pageable pageable) {
        log.debug("소설 목록 조회 요청 - category={}, pageable={}", category, pageable);
        Page<Novel> novels;

        if (category == null) {
            log.debug("카테고리 필터 없음 - 전체 소설 조회 실행");
            novels = novelRepository.findAll(pageable);
        } else {
            log.debug("카테고리 필터 적용 - category={}", category);
            novels = novelRepository.findAllByCategory(category, pageable);
        }

        log.debug("조회된 소설 수: {}", novels.getTotalElements());
        return novels.map(NovelDto.NovelResponse::from);
    }


    /**
     * 제목 또는 작가명으로 소설을 검색합니다.
     *
     * @param keyword  검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과 소설 목록 페이지
     * @throws CustomException 키워드가 비어있을 경우 예외 발생
     */
    public Page<NovelDto.NovelResponse> searchNovel(String keyword, Pageable pageable) {
        log.debug("소설 검색 요청 - keyword='{}', pageable={}", keyword, pageable);

        if (!StringUtils.hasText(keyword)) {
            log.error("입력된 검색 키워드가 없음");
            throw new CustomException(INVALID_KEYWORD);
        }

        Page<Novel> novels = novelRepository.findByTitleOrAuthorName(keyword, pageable);

        log.debug("검색 결과 소설 수: {}", novels.getTotalElements());
        return novels.map(NovelDto.NovelResponse::from);
    }


    /**
     * 소설의 섬네일 이미지를 업로드합니다.
     * 작가 권한이 있는 사용자만 등록할 수 있습니다.
     *
     * @param file 업로드할 이미지 파일
     * @return 업로드된 섬네일 키 정보를 담은 DTO
     * @throws IOException     파일 업로드 중 오류 발생 시
     * @throws CustomException 파일이 비었거나 이미지 형식이 아닌 경우
     */
    @Transactional
    public NovelDto.ThumbnailResponse uploadThumbnail(MultipartFile file) throws IOException {
        log.debug("섬네일 업로드 요청 - 원본 파일명: {}", file.getOriginalFilename());

        validateFile(file);

        String key = s3Service.upload(file, S3_FOLDER_NAME);

        log.debug("섬네일 업로드 완료 - 저장된 key: {}", key);
        return NovelDto.ThumbnailResponse.builder().thumbnailKey(key).build();
    }


    /**
     * 소설을 새로 등록합니다.
     * 작가 권한이 있는 사용자만 등록할 수 있습니다.
     *
     * @param request 소설 등록 요청 DTO
     * @param user    요청자 정보
     * @return 등록된 소설 정보 DTO
     */
    @Transactional
    public NovelDto.NovelInfoResponse createNovel(
            NovelDto.CreateNovelRequest request, CustomUserDetails user
    ) {
        log.debug("소설 등록 요청 - 제목: {}, 작성자: {}", request.getTitle(), user.getUsername());

        Long novelId = generateNovelId();
        log.debug("생성된 소설 고유 번호: {}", novelId);

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

        log.debug("소설 등록 완료 - novelId: {}", novel.getNovelId());
        return NovelDto.NovelInfoResponse.from(novel);
    }


    /**
     * 소설 상세 정보를 조회합니다.
     *
     * @param novelId 조회할 소설 고유번호
     * @return 소설 정보 DTO
     * @throws CustomException 존재하지 않는 소설인 경우
     */
    public NovelDto.NovelInfoResponse getNovel(Long novelId) {
        log.debug("소설 단건 조회 요청 - novelId: {}", novelId);

        Novel novel = findNovelOrThrow(novelId);

        log.debug("조회된 소설 - 제목: {}", novel.getTitle());
        return NovelDto.NovelInfoResponse.from(novel);
    }


    /**
     * 소설의 섬네일을 수정합니다.
     * 작가 권한이 있는 사용자만 수정할 수 있으며, 본인의 작품만 수정할 수 있습니다.
     *
     * @param novelId 수정할 소설 고유번호
     * @param file    새로운 섬네일 파일
     * @param loginId 요청자 로그인 ID
     * @return 수정된 소설 정보 DTO
     * @throws IOException     파일 처리 중 오류 발생 시
     * @throws CustomException 권한 없음 또는 소설 없음
     */
    @Transactional
    public NovelDto.NovelInfoResponse updateThumbnail(
            Long novelId, MultipartFile file, String loginId
    ) throws IOException {
        log.debug("섬네일 수정 요청 - novelId={}, 파일명={}, 요청자={}", novelId, file.getOriginalFilename(), loginId);

        Novel novel = findNovelOrThrow(novelId);
        checkPermissionOrThrow(novel, loginId);

        validateFile(file);

        String oldKey = novel.getThumbnail();
        String newKey = s3Service.update(file, S3_FOLDER_NAME, oldKey);

        log.debug("섬네일 변경 완료 - oldKey={}, newKey={}", oldKey, newKey);
        novel.changeThumbnail(newKey);

        return NovelDto.NovelInfoResponse.from(novel);
    }


    /**
     * 소설 정보를 수정합니다.
     * 작가 권한이 있는 사용자만 수정할 수 있으며, 본인의 작품만 수정할 수 있습니다.
     *
     * @param novelId 수정할 소설 고유번호
     * @param request 수정 요청 DTO
     * @param loginId 요청자 로그인 ID
     * @return 수정된 소설 정보 DTO
     * @throws CustomException 권한 없음 또는 소설 없음
     */
    @Transactional
    public NovelDto.NovelInfoResponse updateNovel(
            Long novelId, NovelDto.UpdateNovelRequest request, String loginId
    ) {
        log.debug("소설 수정 요청 - novelId={}, 요청자={}", novelId, loginId);

        Novel novel = findNovelOrThrow(novelId);
        checkPermissionOrThrow(novel, loginId);

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


    /**
     * 소설을 삭제합니다.
     * 작가 권한이 있는 사용자는 본인의 소설만 삭제할 수 있습니다.
     * 관리자는 모든 소설을 삭제할 수 있습니다.
     *
     * @param novelId 삭제할 소설 ID
     * @param user    요청자 정보
     * @throws IOException     썸네일 삭제 중 오류 발생 시
     * @throws CustomException 권한 없음 또는 소설 없음
     */
    @Transactional
    public void deleteNovel(Long novelId, CustomUserDetails user) throws IOException {
        log.debug("소설 삭제 요청 - novelId={}, 요청자={}", novelId, user.getUsername());

        Novel novel = findNovelOrThrow(novelId);

        boolean isAdmin = user.getUser().getRoles().contains("ROLE_ADMIN");
        boolean isAuthor = novel.getAuthor().getLoginId().equals(user.getUsername());

        if (!isAdmin && !isAuthor) {
            log.error("소설 삭제 권한 없음 - 작가: {}, 요청자: {}", novel.getAuthor().getLoginId(), user.getUsername());
            throw new CustomException(NO_PERMISSION);
        }

        novelRepository.delete(novel);
        s3Service.delete(novel.getThumbnail());

        log.debug("소설 삭제 완료 - novelId={}", novelId);
    }


    /**
     * 외부 노출용 소설 고유번호를 생성합니다.
     */
    private Long generateNovelId() {
        return Math.abs(UUID.randomUUID().getMostSignificantBits());
    }


    /**
     * 소설 고유번호(novelId)로 특정 소설을 조회합니다.
     * 없으면 예외를 던집니다.
     *
     * @param novelId 조회할 소설 고유번호
     * @return 조회된 소설 객체
     * @throws CustomException 존재하지 않는 소설인 경우
     */
    private Novel findNovelOrThrow(Long novelId) {
        return novelRepository.findByNovelId(novelId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 소설: {}", novelId);
                    return new CustomException(NOVEL_NOT_FOUND);
                });
    }


    /**
     * 해당 사용자가 소설의 작가가 맞는지 확인합니다.
     * 아니면 예외를 던집니다.
     *
     * @param novel   소설 객체
     * @param loginId 요청자 로그인 ID
     * @throws CustomException 권한 없음
     */
    private static void checkPermissionOrThrow(Novel novel, String loginId) {
        if (!novel.getAuthor().getLoginId().equals(loginId)) {
            log.error("해당 작업 권한 없음 - 작가: {}, 요청자: {}",
                    novel.getAuthor().getLoginId(), loginId);
            throw new CustomException(NO_PERMISSION);
        }
    }


    /**
     * 파일 유효성을 검사합니다.
     *
     * @param file 검사할 파일
     * @throws CustomException 파일이 비었거나 이미지 형식이 아닌 경우
     */
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


    /**
     * 사용자가 특정 소설을 북마크했는지 확인합니다.
     *
     * @param novelId 소설 고유 ID
     * @param userId  사용자 고유 ID
     * @return 북마크 여부
     */
    public boolean hasBookmarked(Long novelId, Long userId) {
        String bookmarkKey = RedisKeyUtil.userBookmarkKey(userId);
        boolean bookmarked = Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(bookmarkKey, novelId.toString()));

        log.debug("[hasBookmarked] userId={}, novelId={} → hasBookmarked={}", userId, novelId, bookmarked);

        return bookmarked;
    }


    /**
     * 소설을 북마크합니다.
     *
     * @param novelId 북마크할 소설 고유 ID
     * @param userId  사용자 고유 ID
     */
    public void bookmark(Long novelId, Long userId) {
        String bookmarkKey = RedisKeyUtil.userBookmarkKey(userId);

        if (!hasBookmarked(userId, novelId)) {
            redisTemplate.opsForSet().add(bookmarkKey, novelId.toString());
            log.debug("[bookmark] 북마크 완료 - userId={}, novelId={}", userId, novelId);
        } else {
            log.debug("[bookmark] 이미 북마크한 상태 - novelId={}, userId={}", novelId, userId);
        }
    }


    /**
     * 소설 북마크를 해제합니다.
     *
     * @param novelId 북마크 해제할 소설 고유 ID
     * @param userId  사용자 고유 ID
     */
    public void unbookmark(Long novelId, Long userId) {
        String bookmarkKey = RedisKeyUtil.userBookmarkKey(userId);

        if (hasBookmarked(userId, novelId)) {
            redisTemplate.opsForSet().remove(bookmarkKey, novelId.toString());
            log.debug("[unbookmark] 북마크 삭제 완료 - novelId={}, userId={}", novelId, userId);
        } else {
            log.debug("[unbookmark] 이미 북마크가 아닌 상태 - novelId={}, userId={}", novelId, userId);
        }
    }
}