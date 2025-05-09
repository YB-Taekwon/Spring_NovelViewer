package com.ian.novelviewer.novel.application;

import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.exception.ErrorCode;
import com.ian.novelviewer.common.security.CustomUserDetails;
import com.ian.novelviewer.novel.domain.Novel;
import com.ian.novelviewer.novel.domain.NovelRepository;
import com.ian.novelviewer.novel.dto.NovelDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.ian.novelviewer.common.exception.ErrorCode.NOVEL_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelService {

    private final NovelRepository novelRepository;

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
}
