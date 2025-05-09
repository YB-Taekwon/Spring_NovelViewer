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
    public static class EpisodeTitleResponse {
        private Long episodeId;
        private String title;

        public static EpisodeTitleResponse from(Episode episode) {
            return EpisodeTitleResponse.builder()
                    .episodeId(episode.getEpisodeId())
                    .title(episode.getTitle())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EpisodeContentResponse {
        private String content;

        public static EpisodeContentResponse from(Episode episode) {
            return EpisodeContentResponse.builder()
                    .content(episode.getContent())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EpisodeInfoResponse {
        private Long episodeId;
        private String title;
        private String content;

        public static EpisodeInfoResponse from(Episode episode) {
            return EpisodeInfoResponse.builder()
                    .episodeId(episode.getEpisodeId())
                    .title(episode.getTitle())
                    .content(episode.getContent())
                    .build();
        }
    }
}