package com.example.GoCafe.dto;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.MenuCategory;
import com.example.GoCafe.entity.Menu;
import com.pgvector.PGvector;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Vector;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class MenuForm {

    private Long menuId;
    private Cafe cafe;
    private MenuCategory category;
    private String menuName;
    private int menuPrice;
    private boolean menuNew;         // 신메뉴 여부 : 'Y' / 'N'
    private boolean menuRecommanded; // 추천 메뉴 여부 : 'Y' / 'N'
    private String menuPhoto;       // 메뉴 url

    public Menu toEntity() {
        return new Menu(
                menuId,
                cafe,
                category,
                menuName,
                menuPrice,
                menuNew,
                menuRecommanded,
                menuPhoto
        );
    }
}