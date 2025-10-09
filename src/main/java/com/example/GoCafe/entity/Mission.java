package com.example.GoCafe.entity;

import com.example.GoCafe.entity.Cafe;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="MISSION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Mission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="MISSION_ID") private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="CAFE_ID", nullable=false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Cafe cafe;

    @Column(length=100, nullable=false) private String title;
    @Column(length=1000) private String description;

    private LocalDate dueDate;

    @Column(name="SPONSORED_YN", length=1, nullable=false)
    private String sponsoredYn = "N";

    @Column(name="ACTIVE_YN", length=1, nullable=false)
    private String activeYn = "Y";

    @CreationTimestamp
    @Column(nullable=false) private LocalDateTime createdAt;
    @Column(length=100) private String createdBy;
    private LocalDateTime updatedAt;
}