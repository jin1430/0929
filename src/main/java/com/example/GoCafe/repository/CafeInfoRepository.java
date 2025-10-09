package com.example.GoCafe.repository;

import com.example.GoCafe.entity.CafeInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CafeInfoRepository extends JpaRepository<CafeInfo, Long> {
    Optional<CafeInfo> findByCafe_Id(Long cafeId);

    @Query("""
      select ci from CafeInfo ci
      where ci.notice is not null and ci.notice like %:marker%
      order by ci.id desc
    """)
    List<CafeInfo> findByNoticeMarker(@Param("marker") String marker);
    boolean existsByCafe_Id(Long cafeId);

}