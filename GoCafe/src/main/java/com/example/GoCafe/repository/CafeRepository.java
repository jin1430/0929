package com.example.GoCafe.repository;

import com.example.GoCafe.domain.CafeStatus;
import com.example.GoCafe.entity.Cafe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CafeRepository extends JpaRepository<Cafe, Long> {
    List<Cafe> findByStatus(CafeStatus status);

    boolean existsByName(String cafeName);
    boolean existsByNumber(String cafeNumber);

    List<Cafe> findTop8ByOrderByViewsDesc();
}