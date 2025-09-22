package com.example.GoCafe.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RecommendedCafeDto {
    private Long cafeId;
    private String name;
    private String address;
    private String mainPhotoUrl;

    private double score;          // 최종 추천 점수
    private double tagScore;       // 태그 유사도 점수 (0~1)
    private double ratingScore;    // 평점 정규화 점수 (0~1)
    private double popularity;     // 리뷰수 정규화 점수 (0~1)
    private double recency;        // 최근성 점수 (0~1)

    private Double avgRating;      // 평균 별점
    private Integer reviewCount;   // 리뷰 수
}
