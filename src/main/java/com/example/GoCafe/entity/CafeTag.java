package com.example.GoCafe.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@NoArgsConstructor
@Entity
@Table(name = "cafe_tag")
@Getter
@Setter
@AllArgsConstructor
public class CafeTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cafe_tag_id", nullable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafe_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Cafe cafe;

    @Column(name = "category_code", length = 20)
    private String categoryCode;

    @Column(name = "code", length = 20)
    private String code;
}