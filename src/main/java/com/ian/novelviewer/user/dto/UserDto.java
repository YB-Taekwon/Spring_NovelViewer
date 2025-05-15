package com.ian.novelviewer.user.dto;

import com.ian.novelviewer.comment.domain.Comment;
import com.ian.novelviewer.common.enums.Role;
import com.ian.novelviewer.novel.domain.Category;
import com.ian.novelviewer.novel.domain.Novel;
import com.ian.novelviewer.user.domain.User;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import static com.ian.novelviewer.common.enums.Role.ROLE_AUTHOR;

public class UserDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoleRequestRequest {

        @NotBlank(message = "작가명을 입력해주세요.")
        private String authorName;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfoResponse {
        private String loginId;
        private String email;
        private String userName;
        private String authorName;
        private List<Role> roles;

        public static UserInfoResponse from(User user) {
            return UserInfoResponse.builder()
                    .loginId(user.getLoginId())
                    .email(user.getEmail())
                    .userName(user.getUserName())
                    .authorName(user.getRoles().contains(ROLE_AUTHOR) ? user.getAuthorName() : null)
                    .roles(user.getRoles().stream().toList())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookmarksResponse {

        private String title;
        private String thumbnail;
        private String author;
        private Category category;

        public static BookmarksResponse from(Novel novel) {
            return BookmarksResponse.builder()
                    .title(novel.getTitle())
                    .thumbnail(novel.getThumbnail())
                    .author(novel.getAuthor().getAuthorName())
                    .category(novel.getCategory())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommentsResponse {
        private String episode;
        private String content;
        private LocalDateTime createdAt;

        public static UserDto.CommentsResponse from(Comment comment) {
            return CommentsResponse.builder()
                    .episode(comment.getEpisode().getTitle())
                    .content(comment.getContent())
                    .createdAt(comment.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoleRequestResponse {

        private String loginId;
        private String authorName;
        private boolean roleRequestPending;
        private List<Role> roles;

        public static UserDto.RoleRequestResponse from(User user) {
            return RoleRequestResponse.builder()
                    .loginId(user.getLoginId())
                    .authorName(user.getRequestAuthorName())
                    .roleRequestPending(user.isRoleRequestPending())
                    .roles(user.getRoles().stream().toList())
                    .build();
        }
    }
}