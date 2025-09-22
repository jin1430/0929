package com.example.GoCafe.entity;

import com.example.GoCafe.domain.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "notification", indexes = @Index(columnList = "recipient_id,createdAt"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Member recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cafe_id")
    private Cafe cafe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 32, nullable = false) // 필드 및 컬럼명 변경
    private NotificationType notificationType;

    @Column(length = 300)
    private String message;

    @Column(name = "is_read", nullable = false) // 필드 및 컬럼명 변경
    private boolean isRead = false;

    @CreationTimestamp // @PrePersist 대신 사용 가능
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}