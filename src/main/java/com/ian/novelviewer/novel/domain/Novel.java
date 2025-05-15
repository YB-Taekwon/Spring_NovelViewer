package com.ian.novelviewer.novel.domain;

import com.ian.novelviewer.common.base.BaseEntity;
import com.ian.novelviewer.episode.domain.Episode;
import com.ian.novelviewer.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "novels")
public class Novel extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long novelId;

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
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @OneToMany(mappedBy = "novel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Episode> episodes = new ArrayList<>();

    public void changeThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void changeCategory(Category category) {
        this.category = category;
    }
}