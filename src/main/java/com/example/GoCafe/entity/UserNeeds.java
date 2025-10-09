package com.example.GoCafe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@NoArgsConstructor
@Entity
@Getter
@Setter
@AllArgsConstructor
@Table(name = "user_needs")
public class UserNeeds {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_needs_id", nullable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @Column(name = "category_code", length = 20)
    private String categoryCode;

    @Column(name = "needs_code", length = 20) // 필드 및 컬럼명 변경
    private String code;

    @Column(name = "weight", nullable = false)
    private Double weight;
}