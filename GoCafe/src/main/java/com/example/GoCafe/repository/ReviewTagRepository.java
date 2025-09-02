// src/main/java/com/example/GoCafe/repository/ReviewTagRepository.java
package com.example.GoCafe.repository;

import com.example.GoCafe.entity.ReviewTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ReviewTagRepository extends JpaRepository<ReviewTag, Long> {

    /* 좋아요 태그 집계 (GOOD 리뷰만) */
    @Query(value = """
        SELECT t.tag_code AS tagCode, COUNT(*) AS cnt
          FROM review_tag t
          JOIN review r ON r.review_id = t.review_id
         WHERE r.cafe_id = :cafeId
           AND t.tag_category_code = 'LIKE'
           AND r.sentiment = 'GOOD'
         GROUP BY t.tag_code
         ORDER BY cnt DESC
    """, nativeQuery = true)
    List<Object[]> findLikeTagCountsGood(@Param("cafeId") Long cafeId);

    /* 리뷰에 달린 태그 전부 삭제 (연관관계 없이 review_id 정리) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM review_tag WHERE review_id = :reviewId", nativeQuery = true)
    void deleteByReviewId(@Param("reviewId") Long reviewId);

    List<ReviewTag> findByReviewId(Long reviewId);
}
