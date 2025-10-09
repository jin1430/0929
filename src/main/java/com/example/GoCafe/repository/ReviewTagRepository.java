package com.example.GoCafe.repository;

import com.example.GoCafe.entity.ReviewTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewTagRepository extends JpaRepository<ReviewTag, Long> {

    // =========================
    // 인터페이스 프로젝션 (집계용)
    // =========================
    public interface TagCount {
        String getCode();
        Long getCnt();
    }

    /* 좋아요 태그 집계 (GOOD 리뷰만) - JPQL */
    @Query("""
       SELECT t.code AS code, COUNT(t) AS cnt
         FROM ReviewTag t
         JOIN t.review r
        WHERE r.cafe.id = :cafeId
          AND t.categoryCode = 'LIKE'
          AND r.sentiment = 'GOOD'
        GROUP BY t.code
        ORDER BY COUNT(t) DESC
    """)
    List<TagCount> findLikeTagCountsGood(@Param("cafeId") Long cafeId);

    /* 리뷰에 달린 태그 전부 삭제 - JPQL bulk delete */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM ReviewTag t WHERE t.review.id = :reviewId")
    void deleteByReviewId(@Param("reviewId") Long reviewId);

    /* review_id로 태그 조회 - 파생 쿼리 */
    List<ReviewTag> findByReview_Id(Long reviewId);

    // ============================================
    // 리뷰-태그 "행" 단위 조회(coverage 계산용) - JPQL
    //  - 엔티티를 그대로 쓰기 위해 score 필터는 제거
    //  - 서비스 시그니처 유지를 위해 tau 파라미터는 수신만
    // ============================================
    @Query("""
        SELECT
            t.categoryCode AS categoryCode,
            t.code         AS code,
            r.createdAt    AS createdAt
        FROM ReviewTag t
        JOIN t.review r
        WHERE r.cafe.id = :cafeId
        ORDER BY r.createdAt DESC
    """)
    List<ReviewTagProjection> findRowsForCafe(@Param("cafeId") Long cafeId,
                                              @Param("tau") Double tau);

    // JPQL 프로젝션 (score 제거)
    public interface ReviewTagProjection {
        String getCategoryCode();
        String getCode();
        LocalDateTime getCreatedAt();
    }

    boolean existsByReviewIdAndCode(Long reviewId, String code);

    @Query("""
       SELECT t.code AS code, COUNT(t) AS cnt
         FROM ReviewTag t
         JOIN t.review r
        WHERE r.cafe.id = :cafeId
        GROUP BY t.code
        ORDER BY COUNT(t) DESC
    """)
    List<TagCount> findAllTagCountsForCafe(@Param("cafeId") Long cafeId);
}