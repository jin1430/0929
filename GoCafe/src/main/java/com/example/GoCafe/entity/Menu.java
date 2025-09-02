package com.example.GoCafe.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor
@Entity
@Getter
@Setter
@AllArgsConstructor
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id", nullable = false, unique = true)
    private Long menuId;

    @Column(name = "cafe_id", nullable = false)
    private Long cafeId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "menu_name", nullable = false, length = 12)
    private String menuName;

    @Column(name = "menu_price", nullable = false)
    private int menuPrice;

    @Column(name = "menu_new", length = 1)
    private String menuNew;

    @Column(name = "menu_recommanded", length = 1)
    private String menuRecommanded;

    @Column(name = "menu_photo", length = 255)
    private String menuPhoto;

    @OneToMany(mappedBy = "menu", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenuImage> images = new ArrayList<>();
}
