package com.example.GoCafe.repository;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {

    // 특정 카페 전체
    List<Mission> findByCafe(Cafe cafe);

    // 특정 카페 + 활성화
    List<Mission> findByCafeAndActiveYn(Cafe cafe, String activeYn);

    // 전체 + 활성화
    List<Mission> findByActiveYn(String activeYn);

    // 진행중(오늘 기준) + 활성화
    List<Mission> findByActiveYnAndDueDateGreaterThanEqualOrderByIdDesc(String activeYn, LocalDate today);

    // 승인 대기(비활성)
    List<Mission> findByActiveYnOrderByIdDesc(String activeYn);
}
