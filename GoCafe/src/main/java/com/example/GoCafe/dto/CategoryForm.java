package com.example.GoCafe.dto;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.MenuCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryForm {

    private Long categoryId;
    private Cafe cafe;
    private String category;

    public MenuCategory toEntity() {
        return new MenuCategory(categoryId, cafe, category);
    }
}
