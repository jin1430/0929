package com.example.GoCafe.repository;

import com.example.GoCafe.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<MenuCategory, Long> {
}