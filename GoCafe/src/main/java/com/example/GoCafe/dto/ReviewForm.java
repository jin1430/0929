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

    private Long cafeId;
    private String reviewContent;
    private Integer waitingTime;
    private String companionType;
    private Integer taste;
    private String sentiment;
    private List<String> likedTagCodes;
    private LocalDateTime reviewDate;

    public Review toEntity(Cafe cafe, Member member) {
        return Review.builder()
                .cafe(cafe)
                .member(member)
                .content(reviewContent)
                .waitingTime(waitingTime)
                .companionType(companionType)
                .taste(taste)
                .sentiment(sentiment)
                .likedTagCsv(likedTagCodes != null ? String.join(",", likedTagCodes) : null)
                .build();
    }
}