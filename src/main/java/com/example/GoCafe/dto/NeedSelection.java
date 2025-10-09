package com.example.GoCafe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NeedSelection {
    private String categoryCode; // 예: "AMBIENCE"
    private String tagName;      // 예: "아늑함"
    private double weight;       // 항상 1.0으로 저장
}
