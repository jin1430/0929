package com.example.GoCafe.repository;

import com.example.GoCafe.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    // 특정 카페에 속한 모든 메뉴 카테고리를 조회하는 메서드를 추가할 수 있습니다.
    List<MenuCategory> findByCafe_Id(Long cafeId);
}