package com.example.GoCafe.dto;

import com.example.GoCafe.entity.Review;
import com.example.GoCafe.entity.ReviewTag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTagForm {

    private Long id;
    private Long reviewId;
    private String categoryCode;
    private String code;

    public ReviewTag toNewEntity(Review review) {
        ReviewTag entity = new ReviewTag();
        entity.setReview(review);
        entity.setCategoryCode(categoryCode);
        entity.setCode(code);
        return entity;
    }
}