package com.example.GoCafe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "menu_vector")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class MenuVector {

    // MENU 테이블의 PK를 그대로 PK로 쓰는 1:1 매핑 (PK=FK)
    @Id
    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId                           // menu_id를 Menu의 id와 동기화
    @JoinColumn(name = "menu_id")
    private Menu menu;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "vector", nullable = false)
    private byte[] vector;
}