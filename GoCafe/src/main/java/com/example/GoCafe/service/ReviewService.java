// src/main/java/com/example/GoCafe/service/ReviewService.java
package com.example.GoCafe.service;

import com.example.GoCafe.entity.Review;
import com.example.GoCafe.repository.ReviewRepository;
import com.example.GoCafe.support.EntityIdUtil;
import com.example.GoCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public List<Review> findAll() {
        return reviewRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Review findById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Review not found: " + id));
    }

    @Transactional
    public Review create(Review entity) {
        EntityIdUtil.setId(entity, null);
        return reviewRepository.save(entity);
    }

    @Transactional
    public Review update(Long id, Review entity) {
        if (!reviewRepository.existsById(id)) {
            throw new NotFoundException("Review not found: " + id);
        }
        EntityIdUtil.setId(entity, id);
        return reviewRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new NotFoundException("Review not found: " + id);
        }
        reviewRepository.deleteById(id);
    }

    public Review save(Review review) {
        return reviewRepository.save(review);
    }

    // ✅ 추가: 카페 상세에서 사용할 리뷰 목록(최신순), member/cafe 페치
    @Transactional(readOnly = true)
    public List<Review> findByCafeIdWithMember(Long cafeId) {
        return reviewRepository.findByCafe_CafeIdOrderByReviewDateDesc(cafeId);
    }

    // ✅ 추가: 홈에 “최근 올라온 후기”용 (원하면 사용)
    @Transactional(readOnly = true)
    public List<Review> findRecentTop10() {
        return reviewRepository.findTop10ByOrderByReviewDateDesc();
    }
}
