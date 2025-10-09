package com.example.GoCafe.repository;

import com.example.GoCafe.entity.MenuVector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuVectorRepository extends JpaRepository<MenuVector, Long> {
    List<MenuVector> findByMenuIdIn(List<Long> menuIds);
}
