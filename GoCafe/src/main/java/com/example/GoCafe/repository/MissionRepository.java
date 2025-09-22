package com.example.GoCafe.repository;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {

    // 특정 카페에 속한 모든 미션을 조회합니다.
    List<Mission> findByCafe(Cafe cafe);

    // 특정 카페의 미션 중 활성화(Y/N) 상태에 따라 조회합니다.
    List<Mission> findByCafeAndActiveYn(Cafe cafe, String activeYn);

    // 전체 미션 중 활성화(Y/N) 상태에 따라 조회합니다.
    List<Mission> findByActiveYn(String activeYn);
}