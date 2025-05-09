package com.ian.novelviewer.novel.application;

import com.ian.novelviewer.common.security.CustomUserDetails;
import com.ian.novelviewer.novel.domain.Novel;
import com.ian.novelviewer.novel.domain.NovelRepository;
import com.ian.novelviewer.novel.dto.NovelDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelService {

    private final NovelRepository novelRepository;

    @Transactional
    public NovelDto.NovelInfoResponse createNovel(
            NovelDto.CreateNovelRequest request, CustomUserDetails user
    ) {
        Long contentId = generateContentId();

        Novel novel = novelRepository.save(
                Novel.builder()
                        .contentId(contentId)
                        .title(request.getTitle())
                        .thumbnail(request.getThumbnailKey())
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
}
