package com.example.GoCafe.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cafe_image")
public class CafeImage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cafe_id", nullable = false)
    private Cafe cafe;

    @Column(name = "stored_name", nullable = false, length = 120) // 서버 저장 파일명(UUID)
    private String storedName;

    @Column(name = "original_name", nullable = false, length = 255) // 업로드 원본명(한글 포함)
    private String originalName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "url", nullable = false, length = 255) // /uploads/... 로 접근
    private String url;

    @Column(name = "is_main", nullable = false)
    private boolean isMain = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
