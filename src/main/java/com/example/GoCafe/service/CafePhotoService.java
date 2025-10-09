package com.example.GoCafe.service;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafePhoto;
import com.example.GoCafe.repository.CafePhotoRepository;
import com.example.GoCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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

    // 메인 사진 URL만 반환
    public Optional<CafePhoto> getMainPhoto(Long cafeId) {
        return cafePhotoRepository.findMainPhoto(cafeId);
    }


    @Transactional(readOnly = true)
    public List<CafePhoto> list(Long cafeId) {
        return cafePhotoRepository.findByCafe_IdOrderBySortIndexAsc(cafeId);
    }

    @Transactional
    public CafePhoto add(Cafe cafe, FileStorageService.StoredFile stored) {
        CafePhoto photo = new CafePhoto();
        photo.setCafe(cafe);
        photo.setUrl(stored.url());
        photo.setOriginalName(stored.originalName());
        photo.setContentType(stored.contentType());
        photo.setSizeBytes(stored.sizeBytes());
        int next = cafePhotoRepository.findFirstByCafe_IdOrderBySortIndexDesc(cafe.getId())
                .map(p -> (p.getSortIndex() == null ? 0 : p.getSortIndex()) + 1)
                .orElse(0);
        photo.setSortIndex(next);
        return cafePhotoRepository.save(photo);
    }

    @Transactional
    public void delete(Long photoId) {
        if (!cafePhotoRepository.existsById(photoId)) throw new NotFoundException("사진 없음");
        cafePhotoRepository.deleteById(photoId);
    }

    @Transactional
    public void setMain(Long cafeId, Long photoId) {
        // 기존 대표 해제
        cafePhotoRepository.findByCafe_IdAndIsMainTrue(cafeId).ifPresent(p -> {
            p.setIsMain(false);
            cafePhotoRepository.save(p);
        });
        // 새 대표 설정
        CafePhoto p = cafePhotoRepository.findById(photoId).orElseThrow(() -> new NotFoundException("사진 없음"));
        p.setIsMain(true);
        cafePhotoRepository.save(p);
    }

    @Transactional
    public static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) return null;
        String u = url.trim();
        String lower = u.toLowerCase();
        // 절대경로면 그대로
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("data:")) {
            return u;
        }
        // 상대경로면 선행 슬래시 보장
        return u.startsWith("/") ? u : ("/" + u);
    }

}
