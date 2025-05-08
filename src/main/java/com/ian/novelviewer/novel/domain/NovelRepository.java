package com.ian.novelviewer.novel.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Long> {
    Optional<Novel> findByContentId(Long contentId);

    Page<Novel> findAllByCategory(Category category, Pageable pageable);

    @Query("""
                select distinct n from Novel n
                join n.author u
                where n.title like concat('%', :keyword, '%')
                   or u.nickname like concat('%', :keyword, '%')
            """)
    Page<Novel> findByTitleOrAuthorName(String keyword, Pageable pageable);
}