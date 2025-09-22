package com.example.GoCafe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@NoArgsConstructor
@Entity
@Table(
        name = "review_tag",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_review_tag_review_code", columnNames = {"review_id", "tag_code"}) // 컬럼명 변경
        }
)
@Getter @Setter
@AllArgsConstructor
public class ReviewTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_tag_id", nullable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Review review;

    @Column(name = "category_code", length = 20)
    private String categoryCode;

    @Column(name = "tag_code", length = 20) // 필드 및 컬럼명 변경
    private String code;
}