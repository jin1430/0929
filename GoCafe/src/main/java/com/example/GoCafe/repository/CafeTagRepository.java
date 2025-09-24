package com.example.GoCafe.repository;

import com.example.GoCafe.entity.CafeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CafeTagRepository extends JpaRepository<CafeTag, Long> {

    Optional<CafeTag> findByCafe_IdAndTagCode(Long cafeId, String tagCode);
    List<CafeTag> findByCafe_Id(Long cafeId);
    List<CafeTag> findByCafe_IdOrderByRankNoAsc(Long cafeId);
    void deleteByCafe_Id(Long cafeId);


}
