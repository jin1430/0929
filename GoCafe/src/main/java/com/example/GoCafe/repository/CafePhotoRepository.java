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

    Optional<CafePhoto> findFirstByCafe_IdAndIsMainTrueOrderBySortIndexAsc(Long cafeId);
    Optional<CafePhoto> findFirstByCafe_IdOrderBySortIndexAsc(Long cafeId);

    boolean existsByCafe_IdAndIsMainTrue(Long cafeId);

    long countByCafe_Id(Long cafeId);

    @Query(value = """
        SELECT *
        FROM (
          SELECT cp.*,
                 ROW_NUMBER() OVER (
                   PARTITION BY cp.cafe_id
                   ORDER BY CASE WHEN cp.is_main = 1 THEN 0 ELSE 1 END,
                            cp.sort_index ASC,
                            cp.cafe_photo_id ASC
                 ) AS rn
          FROM cafe_photo cp
        )
        WHERE rn = 1
        """, nativeQuery = true)
    List<CafePhoto> findMainPhotosForAllCafes();

    @Query("select p from CafePhoto p " +
            "where p.cafe.id in :cafeIds " +
            "order by p.cafe.id asc, p.isMain desc, p.sortIndex asc")
    List<CafePhoto> findForCafeIdsOrderByMainThenSort(@Param("cafeIds") Collection<Long> cafeIds);

}