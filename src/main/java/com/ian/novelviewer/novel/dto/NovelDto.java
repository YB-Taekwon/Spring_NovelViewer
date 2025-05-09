package com.ian.novelviewer.novel.dto;

import com.ian.novelviewer.novel.domain.Category;
import com.ian.novelviewer.novel.domain.Novel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class NovelDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateNovelRequest {

        @NotBlank(message = "작품명을 입력해주세요.")
        @Size(min = 2, max = 50)
        private String title;

        @NotBlank
        @Size(min = 2, max = 1000)
        private String description;

        @NotNull
        private Category category;

        @NotBlank
        private String thumbnail;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateNovelRequest {

        @Size(min = 2, max = 50)
        private String title;

        @Size(min = 2, max = 1000)
        private String description;

        private Category category;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ThumbnailResponse {
        private String thumbnailKey;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NovelResponse {

        private String thumbnail;
        private String title;
        private String author;

        public static NovelResponse from(Novel novel) {
            return NovelResponse.builder()
                    .thumbnail(novel.getThumbnail())
                    .title(novel.getTitle())
                    .author(novel.getAuthor().getNickname())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NovelInfoResponse {

        private Long novelId;
        private String thumbnail;
        private String title;
        private String description;
        private Category category;
        private String author;

        public static NovelInfoResponse from(Novel novel) {
            return NovelInfoResponse.builder()
                    .novelId(novel.getNovelId())
                    .thumbnail(novel.getThumbnail())
                    .title(novel.getTitle())
                    .description(novel.getDescription())
                    .category(novel.getCategory())
                    .author(novel.getAuthor().getNickname())
                    .build();
        }
    }
}