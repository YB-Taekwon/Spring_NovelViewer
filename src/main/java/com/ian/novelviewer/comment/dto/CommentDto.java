package com.ian.novelviewer.comment.dto;

import com.ian.novelviewer.comment.domain.Comment;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CommentDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateCommentRequest {

        @NotBlank
        private String content;

        private Long parentId;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateCommentRequest {

        @NotBlank
        private String content;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommentResponse {
        private Long id;
        private String content;
        private Long likes;
        private String writer;
        private LocalDateTime createdAt;

        public static CommentResponse from(Comment comment, Long likes) {
            return CommentResponse.builder()
                    .id(comment.getId())
                    .content(comment.getContent())
                    .likes(likes != null ? likes : 0L)
                    .writer(comment.getUser().getLoginId())
                    .createdAt(comment.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommentInfoResponse {
        private Long id;
        private String content;
        private Long likes;
        private String writer;
        private Long parentId;
        private LocalDateTime createdAt;
        private List<CommentResponse> children;

        public static CommentInfoResponse from(Comment comment, Long likes) {
            return CommentInfoResponse.builder()
                    .id(comment.getId())
                    .content(comment.getContent())
                    .likes(likes != null ? likes : 0L)
                    .writer(comment.getUser().getLoginId())
                    .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                    .createdAt(comment.getCreatedAt())
                    .children(
                            Optional.ofNullable(comment.getChildren())
                                    .orElseGet(Collections::emptyList)
                                    .stream()
                                    .map(child -> CommentResponse.from(child, null))
                                    .toList()
                    )
                    .build();
        }
    }
}