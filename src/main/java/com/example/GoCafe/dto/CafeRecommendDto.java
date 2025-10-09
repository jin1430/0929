package com.example.GoCafe.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CafeRecommendDto {
    private Long cafeId;
    private String name;
    private String address;
    private Long views;
    private String photoUrl;

    private double score;              // 성별 가중치 적용 총점
    private List<String> topTags;      // 상위 태그(선택)
}
