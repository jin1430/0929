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

    // ✅ 결과를 DTO처럼 사용할 수 있는 인터페이스 프로젝션
    interface TagCount {
        String getCode();
        Long getCnt();
    }

    /* 좋아요 태그 집계 (GOOD 리뷰만) */
    // ✅ 반환 타입을 List<Object[]> 대신 List<TagCount>로 변경하여 타입 안정성 확보
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

    /* 리뷰에 달린 태그 전부 삭제 (연관관계 없이 review_id 정리) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM review_tag WHERE review_id = :reviewId", nativeQuery = true)
    void deleteByReviewId(@Param("reviewId") Long reviewId);

    List<ReviewTag> findByReview_Id(Long reviewId);

    @Query(
            value = """
      select rt.tag_code, count(*) as cnt, avg(rt.score) as avg_score, sum(rt.score) as sum_score
      from review_tag rt
      join review r on r.review_id = rt.review_id
      where r.cafe_id = :cafeId
      and rt.score >= :minScore
      group by rt.tag_code
      order by avg_score desc
     """,
            nativeQuery = true
    )
    List<ReviewTagProjection> findRowsForCafe(@Param("cafeId") Long cafeId, @Param("tau") Double tau);

    interface ReviewTagProjection {
        String getCategoryCode();
        String getCode();
        Double getScore();
        LocalDateTime getCreatedAt();
    }

}