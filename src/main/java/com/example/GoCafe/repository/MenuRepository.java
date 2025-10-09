package com.example.GoCafe.repository;

import com.example.GoCafe.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByCafe_Id(Long cafeId);

    List<Menu> findByCafeId(Long cafeId);
}