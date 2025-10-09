package com.example.GoCafe.repository;

import com.example.GoCafe.entity.CafePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CafePhotoRepository extends JpaRepository<CafePhoto, Long> {

    List<CafePhoto> findByCafe_Id(Long cafeId);
    List<CafePhoto> findByCafe_IdOrderBySortIndexAsc(Long cafeId);

    // ✅ 필드명이 isMain으로 변경되어 메서드명을 직관적으로 사용할 수 있습니다.
    Optional<CafePhoto> findFirstByCafe_IdAndIsMainTrueOrderBySortIndexAsc(Long cafeId);

    Optional<CafePhoto> findFirstByCafe_IdOrderBySortIndexAsc(Long cafeId);

    // ✅ 필드명 isMain에 맞춰 수정
    boolean existsByCafe_IdAndIsMainTrue(Long cafeId);

    long countByCafe_Id(Long cafeId);

    // (서브쿼리에 alias 필수: MySQL 등)
    // Oracle에서는 큰따옴표로 컬럼명을 감싸는 것이 표준에 더 가깝습니다.
    @Query(value = """
        SELECT * FROM (
          SELECT cp.*,
                 ROW_NUMBER() OVER (
                   PARTITION BY cp."cafe_id"
                   ORDER BY CASE WHEN cp."is_main" = 1 THEN 0 ELSE 1 END,
                            cp."sort_index" ASC,
                            cp."cafe_photo_id" ASC
                 ) AS rn
          FROM "cafe_photo" cp
        ) t
        WHERE rn = 1
        """, nativeQuery = true)
    List<CafePhoto> findMainPhotosForAllCafes();

    // ✅ p.isMain으로 필드명을 정확히 사용
    @Query("select p from CafePhoto p " +
            "where p.cafe.id in :cafeIds " +
            "order by p.cafe.id asc, p.isMain desc, p.sortIndex asc")
    List<CafePhoto> findForCafeIdsOrderByMainThenSort(@Param("cafeIds") Collection<Long> cafeIds);

    // Oracle 12c+ 에서 지원하는 FETCH FIRST 문법을 사용. 21c에서 문제 없습니다.
    @Query(value = """
    SELECT p.*
      FROM "cafe_photo" p
     WHERE p."cafe_id" = :cafeId
     ORDER BY CASE WHEN p."is_main" = 1 THEN 0 ELSE 1 END, p."sort_index" ASC
     FETCH FIRST 1 ROWS ONLY
    """, nativeQuery = true)
    Optional<CafePhoto> findMainPhoto(@Param("cafeId") Long cafeId);



    Optional<CafePhoto> findFirstByCafe_IdOrderBySortIndexDesc(Long cafeId);

    // ✅ 필드명 isMain에 맞춰 수정
    Optional<CafePhoto> findByCafe_IdAndIsMainTrue(Long cafeId);

    Optional<CafePhoto> findFirstByCafe_IdOrderByIsMainDescIdAsc(Long cafeId);

    List<CafePhoto> findByCafe_IdIn(Collection<Long> cafeIds);
}