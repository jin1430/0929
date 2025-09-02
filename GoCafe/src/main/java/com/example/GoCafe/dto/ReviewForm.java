// src/main/java/com/example/GoCafe/dto/ReviewForm.java
package com.example.GoCafe.dto;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewForm {

    private Long cafeId;                // 컨트롤러에서 Cafe 조회 용
    private String reviewContent;

    // 설문/메타
    private Integer waitingTime;        // 분
    private String companionType;       // SOLO/FRIEND/DATE/FAMILY/BUSINESS
    private Integer taste;              // 1~5
    private String sentiment;           // GOOD/BAD
    private List<String> likedTagCodes; // "맛있어요" 등 코드 리스트

    // 필요 시 날짜를 외부에서 세팅하지 않으면 엔티티 @PrePersist 가 채웁니다.
    private LocalDateTime reviewDate;

    /** 컨트롤러/서비스에서 Cafe, Member 엔티티를 조회한 뒤 주입하세요. */
    public Review toEntity(Cafe cafe, Member member) {
        return Review.builder()
                .cafe(cafe)
                .member(member)
                .reviewContent(reviewContent)
                .reviewGood(0)   // int (엔티티 기본값도 0이지만 명시해도 됨)
                .reviewBad(0)    // int
                .waitingTime(waitingTime)
                .companionType(companionType)
                .taste(taste)
                .sentiment(sentiment)
                .likedTagCsv(likedTagCodes != null ? String.join(",", likedTagCodes) : null)
                .reviewDate(reviewDate) // null이면 엔티티 @PrePersist에서 now() 주입
                .build();
    }
}
