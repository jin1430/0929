package com.example.GoCafe.repository;

import com.example.GoCafe.entity.CafeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CafeTagRepository extends JpaRepository<CafeTag, Long> {
}
