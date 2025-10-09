package com.example.GoCafe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ReviewTagForm {
    private String categoryCode;
    private String tagCode;
}
