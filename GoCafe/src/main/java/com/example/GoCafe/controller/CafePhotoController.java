package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafePhoto;
import com.example.GoCafe.repository.CafePhotoRepository;
import com.example.GoCafe.repository.CafeRepository;
import com.example.GoCafe.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cafes")
public class CafePhotoController {

    private final CafeRepository cafeRepository;
    private final CafePhotoRepository cafePhotoRepository; // CafePhoto용 리포지토리라고 가정
    private final FileStorageService storage;

    @PostMapping("/{cafeId}/photos")
    @Transactional
    public ResponseEntity<?> upload(@PathVariable Long cafeId,
                                    @RequestParam("files") List<MultipartFile> files,
                                    @RequestParam(value = "setMain", required = false, defaultValue = "false") boolean setMain) throws Exception {

        // 카페 조회
        Cafe cafe = cafeRepository.findById(cafeId).orElseThrow();

        // 기존 사진 목록과 메인 여부 확인
        List<CafePhoto> existing = cafePhotoRepository.findByCafe_Id(cafeId);
        boolean hasMain = existing.stream().anyMatch(CafePhoto::isMain);
        boolean mainAssigned = false; // 이번 업로드에서 메인을 한 번만 지정하기 위한 플래그
        int nextOrder = existing.size(); // 정렬값으로 사용할 카운터(필드명이 originalName:int 이므로 여기에 매핑)

        for (MultipartFile f : files) {
            // 파일 저장 후 URL 획득
            var sf = storage.store(f, "cafes/" + cafeId); // sf.url() 등 메타 반환 가정

            // CafePhoto 엔티티 생성 및 값 세팅
            CafePhoto img = new CafePhoto();
            img.setCafe(cafe);                  // FK 연결
            img.setUrl(sf.url());               // 저장된 경로/URL
            img.setSortIndex(nextOrder++);   // 정렬용 숫자 저장(필드명이 originalName:int)

            // 메인 지정 로직: setMain=true 이거나 기존에 메인이 없으면 이번 업로드 중 첫 장을 메인으로
            if ((setMain && !mainAssigned) || !hasMain) {
                // 기존 메인 해제
                if (!mainAssigned && (setMain || !hasMain)) {
                    for (CafePhoto p : existing) {
                        if (p.isMain()) p.setMain(false);
                    }
                    mainAssigned = true;
                    hasMain = true;
                }
                img.setMain(true);
            } else {
                img.setMain(false);
            }

            // 저장
            cafePhotoRepository.save(img);
        }

        // 업로드 후 전체 목록 반환
        return ResponseEntity.ok(cafePhotoRepository.findByCafe_Id(cafeId));
    }

    @PatchMapping("/photos/{photoId}/main")
    @Transactional
    public ResponseEntity<?> setMain(@PathVariable Long photoId) {
        // 대상 사진과 카페 조회
        CafePhoto target = cafePhotoRepository.findById(photoId).orElseThrow();
        Cafe cafe = target.getCafe();

        // 동일 카페의 모든 사진을 순회하며 메인 플래그 갱신
        List<CafePhoto> all = cafePhotoRepository.findByCafe_Id(cafe.getId());
        for (CafePhoto p : all) {
            p.setMain(p.getId().equals(photoId));
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{cafeId}/photos")
    public List<CafePhoto> list(@PathVariable Long cafeId) {
        // 카페별 사진 전체 조회
        return cafePhotoRepository.findByCafe_Id(cafeId);
    }

    @DeleteMapping("/photos/{photoId}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long photoId) {
        // 사진 삭제
        cafePhotoRepository.deleteById(photoId);
        return ResponseEntity.noContent().build();
    }
}
