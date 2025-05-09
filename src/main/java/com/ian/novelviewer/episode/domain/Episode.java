package com.ian.novelviewer.episode.domain;

import com.ian.novelviewer.common.base.BaseEntity;
import com.ian.novelviewer.novel.domain.Novel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "episodes")
public class Episode extends BaseEntity {

    @Column(nullable = false)
    private Long episodeId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id", nullable = false)
    private Novel novel;
}