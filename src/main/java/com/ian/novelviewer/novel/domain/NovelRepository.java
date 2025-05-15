package com.ian.novelviewer.novel.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Long> {
    /**
     * 소설 고유번호(novelId)를 기준으로 단일 소설을 조회
     */
    Optional<Novel> findByNovelId(Long novelId);

    /**
     * 주어진 카테고리에 해당하는 소설 목록을 페이징하여 조회
     */
    Page<Novel> findAllByCategory(Category category, Pageable pageable);

    /**
     * 제목 또는 작가명에 주어진 키워드가 포함된 소설을 검색
     */
    @Query("""
                select distinct n from Novel n
                join n.author u
                where n.title like concat('%', :keyword, '%')
                   or u.authorName like concat('%', :keyword, '%')
            """)
    Page<Novel> findByTitleOrAuthorName(String keyword, Pageable pageable);

    /**
     * 주어진 소설 고유번호 목록(novelIds)에 해당하는 소설들을 페이징하여 조회
     */
    @Query("select n from Novel n where n.novelId in :novelIds")
    Page<Novel> findAllByNovelIdIn(List<Long> novelIds, Pageable pageable);
}