// src/main/java/com/example/GoCafe/service/ReviewService.java
package com.example.GoCafe.service;

import com.example.GoCafe.dto.MyReviewItem;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Review;
import com.example.GoCafe.repository.CafePhotoRepository;
import com.example.GoCafe.repository.ReviewRepository;
import com.example.GoCafe.support.EntityIdUtil;
import com.example.GoCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CafePhotoRepository cafePhotoRepository;

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

    // 카페 상세에서 사용할 리뷰 목록(최신순)
    @Transactional(readOnly = true)
    public List<Review> findByCafeIdWithMember(Long cafeId) {
        return reviewRepository.findByCafe_IdOrderByCreatedAtDesc(cafeId);
    }

    // 홈의 최근 올라온 후기
    @Transactional(readOnly = true)
    public List<Review> findRecentTop10() {
        return reviewRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public Page<MyReviewItem> findMyReviews(Long memberId, Pageable pageable) {
        return reviewRepository.findByMember_IdOrderByCreatedAtDesc(memberId, pageable)
                .map(this::toItem);
    }

    private MyReviewItem toItem(Review review) {
        Cafe cafe = review.getCafe();
        Long cafeId = (cafe != null ? cafe.getId() : null);
        String cafeName = (cafe != null ? cafe.getName() : "(알 수 없음)");

        // 카페 사진 url
        String cafeMainPhotoUrl = cafePhotoRepository.findMainPhoto(cafe.getId()).getUrl();
        try {
            // 예시: c.getCafeThumb() 가 있으면 /images/cafe/{file} 로 만든다
            var method = Cafe.class.getMethod("getCafeThumb");
            Object v = (cafe != null ? method.invoke(cafe) : null);
            if (v != null) {
                String s = String.valueOf(v);
                if (!s.isBlank()) cafeMainPhotoUrl = "/images/cafe/" + s;
            }
        } catch (Exception ignore) {
            // 필드가 없으면 null -> 템플릿에서 NO IMG
        }

        return new MyReviewItem(
                review.getId(),
                cafeId,
                cafeName,
                cafeMainPhotoUrl,
                safe(review.getContent()),
                review.getCreatedAt(),
                review.getGood(),
                review.getBad()
        );
    }

    private static String safe(String s) { return s == null ? "" : s; }

}
