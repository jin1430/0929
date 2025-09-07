package com.example.GoCafe.dto;

import com.example.GoCafe.entity.Review;
import com.example.GoCafe.entity.ReviewPhoto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewPhotoForm {

    private Long reviewId;
    private String url;
    private int sortIndex;

    public ReviewPhoto toNewEntity(Review review) {
        ReviewPhoto entity = new ReviewPhoto();
        entity.setReview(review);
        entity.setUrl(url);
        entity.setSortIndex(sortIndex);
        return entity;
    }
}