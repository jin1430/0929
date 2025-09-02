package com.example.GoCafe.repository;


import com.example.GoCafe.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("""
        select r
        from Review r
        join fetch r.member
        join fetch r.cafe
        where r.cafe.cafeId = :cafeId
        order by r.reviewDate desc
    """)
    List<Review> findByCafeIdWithMember(@Param("cafeId") Long cafeId);

    @EntityGraph(attributePaths = {"member", "cafe"})
    List<Review> findByCafe_CafeIdOrderByReviewDateDesc(Long cafeId);

    @EntityGraph(attributePaths = {"member", "cafe"})
    List<Review> findTop10ByOrderByReviewDateDesc();
    int countByCafe_CafeIdAndSentiment(Long cafeId, String sentiment);
    List<Review> findByCafe_CafeIdOrderByReviewIdDesc(Long cafeId);
}