package com.example.GoCafe.repository;

import com.example.GoCafe.entity.UserNeeds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NeedsRepository extends JpaRepository<UserNeeds, Long> {
}