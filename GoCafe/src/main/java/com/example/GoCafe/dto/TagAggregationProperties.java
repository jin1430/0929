package com.example.GoCafe.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.HashMap;
import java.util.Map;

// src/main/java/com/example/GoCafe/dto/TagAggregationProperties.java
@Getter
@Setter
@ConfigurationPropertiesScan("com.example.GoCafe")
@ConfigurationProperties(prefix = "gocafe.tag")
public class TagAggregationProperties {
    private int topK = 5;
    private double tau = 0.6;
    private int nMin = 10;
    private int sMin = 3;
    private double delta = 0.15;

    // coverage 하한
    private double cMin = 0.10;          // 레거시 전역 기본값
    private double cminDefault = 0.10;   // 카테고리별 미설정 시 기본
    private Map<String, Double> cmin = new HashMap<>();

    // VIBE
    private int vibeSupportMin = 5;
    private double vibeShareMin = 0.40;

    // PRICE
    private double deltaPrice = 0.10;
    private double sMinPriceRatio = 0.2;

    /** 카테고리별 coverage 하한값을 안전하게 꺼내는 헬퍼 */
    public double getEffectiveCmin(String category) {
        return cmin.getOrDefault(category, cminDefault);
    }
}
