package com.example.GoCafe.dto;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafeTag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CafeTagForm {

    private Long id;
    private Long cafeId;
    private String categoryCode;
    private String code;

    public CafeTag toNewEntity(Cafe cafe) {
        CafeTag entity = new CafeTag();
        entity.setCafe(cafe);
        entity.setCategoryCode(categoryCode);
        entity.setTagCode(code);
        return entity;
    }
}