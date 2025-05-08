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

import java.util.UUID;

import static com.ian.novelviewer.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelService {

    private final NovelRepository novelRepository;

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
    public NovelDto.NovelInfoResponse createNovel(
            NovelDto.CreateNovelRequest request, String imageKey, CustomUserDetails user
    ) {
        Long contentId = generateContentId();

        Novel novel = novelRepository.save(
                Novel.builder()
                        .contentId(contentId)
                        .thumbnail(imageKey)
                        .title(request.getTitle())
                        .description(request.getDescription())
                        .category(request.getCategory())
                        .author(user.getUser())
                        .build()
        );

        return NovelDto.NovelInfoResponse.from(novel);
    }

    private Long generateContentId() {
        UUID uuid = UUID.randomUUID();

        return Math.abs(uuid.getMostSignificantBits());
    }

    public NovelDto.NovelInfoResponse getNovel(Long contentId) {
        Novel novel = novelRepository.findByContentId(contentId)
                .orElseThrow(() -> {
                    log.warn("작품을 찾을 수 없습니다. {}", contentId);
                    return new CustomException(NOVEL_NOT_FOUND);
                });

        return NovelDto.NovelInfoResponse.from(novel);
    }

    @Transactional
    public NovelDto.NovelInfoResponse updateNovel(
            Long contentId, NovelDto.UpdateNovelRequest request, String imageKey, CustomUserDetails user
    ) {
        Novel novel = novelRepository.findByContentId(contentId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 작품: {}", contentId);
                    return new CustomException(NOVEL_NOT_FOUND);
                });

        if (!novel.getAuthor().getLoginId().equals(user.getUser().getLoginId())) {
            throw new CustomException(NO_PERMISSION);
        }

        if (StringUtils.hasText(imageKey)) {
            log.info("섬네일 변경: {}", imageKey);
            novel.changeThumbnail(imageKey);
        }

        if (request.getTitle() != null && !request.getTitle().isEmpty()) {
            log.info("제목 변경: {}", request.getTitle());
            novel.changeTitle(request.getTitle());
        }

        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            log.info("소개글 변경: {}", request.getDescription());
            novel.changeDescription(request.getDescription());
        }

        if (request.getCategory() != null) {
            log.info("카테고리 변경: {}", request.getCategory());
            novel.changeCategory(request.getCategory());
        }

        return NovelDto.NovelInfoResponse.from(novel);
    }

    @Transactional
    public void deleteNovel(Long contentId, CustomUserDetails user) {
        Novel novel = novelRepository.findByContentId(contentId)
                .orElseThrow(() -> {
                    log.error("작품을 찾을 수 없습니다. {}", contentId);
                    return new CustomException(NOVEL_NOT_FOUND);
                });

        if (!novel.getAuthor().getLoginId().equals(user.getUser().getLoginId())) {
            log.error("작품 삭제 권한 없음: {}", user.getUser().getLoginId());
            throw new CustomException(NO_PERMISSION);
        }

        novelRepository.deleteByContentId(contentId);
        log.info("삭제 성공");
    }
}
