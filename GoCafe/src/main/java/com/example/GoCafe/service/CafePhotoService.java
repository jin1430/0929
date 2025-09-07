package com.example.GoCafe.service;

import com.example.GoCafe.entity.CafePhoto;
import com.example.GoCafe.repository.CafePhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CafePhotoService {

    private final CafePhotoRepository cafePhotoRepository;

    public List<CafePhoto> findMainPhotosForAllCafes() {
        return cafePhotoRepository.findMainPhotosForAllCafes();
    }

    public List<CafePhoto> findForCafeIdsOrderByMainThenSort(Set<Long> topIds) {
        return cafePhotoRepository.findForCafeIdsOrderByMainThenSort(topIds);
    }
}
