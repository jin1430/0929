package com.example.GoCafe.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MenuUpdate {
    @Size(max = 12)
    private String name;
    private Integer price;
    private Boolean isNew;
    private Boolean isRecommended;
}