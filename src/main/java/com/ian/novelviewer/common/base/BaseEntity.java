package com.ian.novelviewer.common.base;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 공통 엔티티 상속 클래스입니다.
 * 모든 엔티티에 ID, 생성일, 수정일 필드를 제공합니다.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    // 테이블의 기본 아이디 (자동 증가)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 생성 시각
    @CreatedDate
    private LocalDateTime createdAt;

    // 마지막 수정 시각
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
