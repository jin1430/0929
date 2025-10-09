package com.example.GoCafe.repository;

import com.example.GoCafe.entity.CafeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CafeTagRepository extends JpaRepository<CafeTag, Long> {

    Optional<CafeTag> findByCafe_IdAndTagCode(Long cafeId, String tagCode);
    List<CafeTag> findByCafe_Id(Long cafeId);
    List<CafeTag> findByCafe_IdOrderByRankNoAsc(Long cafeId);
    void deleteByCafe_Id(Long cafeId);

    @Query("select ct from CafeTag ct where ct.cafe.id = :cafeId")
    List<CafeTag> findByCafeId(@Param("cafeId") Long cafeId);

    // ✅ 후보 카페들에 대한 태그를 한 번에 가져오기 (N+1 방지)
    @Query("select ct from CafeTag ct where ct.cafe.id in :cafeIds")
    List<CafeTag> findByCafeIdIn(@Param("cafeIds") Set<Long> cafeIds);

    List<CafeTag> findByCafeIdOrderByScoreDesc(Long cafeId);

    List<CafeTag> findTop4ByCafeIdOrderByScoreDesc(Long cafeId);
}
