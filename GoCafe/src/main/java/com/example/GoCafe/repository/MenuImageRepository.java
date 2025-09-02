package com.example.GoCafe.repository;

import com.example.GoCafe.entity.MenuImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MenuImageRepository extends JpaRepository<MenuImage, Long> {
    List<MenuImage> findByMenu_MenuId(Long menuId);
}
