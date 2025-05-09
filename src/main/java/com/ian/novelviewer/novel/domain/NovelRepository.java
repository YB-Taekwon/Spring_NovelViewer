package com.ian.novelviewer.novel.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Long> {
    Optional<Novel> findByContentId(Long contentId);
}