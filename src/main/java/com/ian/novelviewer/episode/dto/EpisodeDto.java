package com.ian.novelviewer.episode.dto;

import com.ian.novelviewer.episode.domain.Episode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class EpisodeDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateEpisodeRequest {
        @NotBlank
        private String title;

        @NotBlank
        private String content;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EpisodeInfoResponse {
        private Long id;
        private String title;
        private String content;

        public static EpisodeInfoResponse from(Episode episode) {
            return EpisodeInfoResponse.builder()
                    .id(episode.getId())
                    .title(episode.getTitle())
                    .content(episode.getContent())
                    .build();
        }
    }
}
