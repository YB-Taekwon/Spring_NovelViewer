package com.ian.novelviewer.novel.domain;

import com.ian.novelviewer.common.base.BaseEntity;
import com.ian.novelviewer.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "novels")
public class Novel extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long contentId;

    @Column(name = "thumbnail", nullable = false, length = 1000)
    private String thumbnail;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;
}