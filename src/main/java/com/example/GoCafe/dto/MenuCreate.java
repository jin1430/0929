package com.example.GoCafe.dto;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Menu;
import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MenuCreate {

    @NotNull
    private Long cafeId;

    @NotBlank
    @Size(max = 12)
    private String name;

    @Min(0)
    private int price;

    // boolean 필드명은 엔티티와 동일하게 유지 (Menu#setNew, Menu#isNew)
    private boolean isNew;
    private boolean isRecommended;

    // 컨트롤러에서 Cafe 조회 후 엔티티로 변환
    public Menu toEntity(Cafe cafe) {
        Menu menu = new Menu();
        menu.setCafe(cafe);
        menu.setName(this.name);
        menu.setPrice(this.price);
        menu.setNew(this.isNew);
        menu.setRecommended(this.isRecommended);
        return menu;
    }
}
