package com.example.GoCafe.repository;

import com.example.GoCafe.entity.CafeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CafeImageRepository extends JpaRepository<CafeImage, Long> {
    List<CafeImage> findByCafe_CafeId(Long cafeId);
}
