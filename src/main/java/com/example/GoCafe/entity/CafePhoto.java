package com.example.GoCafe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "cafe_photo")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CafePhoto {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cafe_photo_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafe_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Cafe cafe;

    @Column(name = "photo_url", nullable = false, length = 255)
    private String url;

    @Column(name = "original_name", length = 120)
    private String originalName;

    @Column(name = "content_type", length = 60)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "sort_index")
    private Integer sortIndex = 0;

    @Column(name = "is_main")
    private Boolean isMain = Boolean.FALSE; // 필드명 변경
}