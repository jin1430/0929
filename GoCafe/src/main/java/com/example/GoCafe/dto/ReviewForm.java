package com.example.GoCafe.dto;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.Review;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewForm {

    private Long cafeId;

    private String reviewContent;

    private Integer waitingTime;
    private String companionType;

    /** ← 추가: 모달에서 name="rating" 으로 넘어오는 값 */
    private Integer rating;

    /** 기존: 실제 DB의 평점 역할(1~5) */
    private Integer taste;

    private String sentiment;
    private List<String> likedTagCodes;
    private LocalDateTime reviewDate;

    public Review toEntity(Cafe cafe, Member member) {
        // rating이 들어오면 taste로 흡수
        Integer tasteValue = (taste != null ? taste : rating);

        return Review.builder()
                .cafe(cafe)
                .member(member)
                .content(reviewContent)
                .good(0)
                .bad(0)
                .waitingTime(waitingTime)
                .companionType(companionType)
                .taste(tasteValue)              // ★ 핵심
                .sentiment(sentiment)
                .likedTagCsv(likedTagCodes != null ? String.join(",", likedTagCodes) : null)
                .createdAt(reviewDate)
                .build();
    }
}
