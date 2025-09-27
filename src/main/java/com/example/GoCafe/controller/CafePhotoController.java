// src/main/java/com/example/GoCafe/controller/CafePhotoController.java
package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafePhoto;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.repository.CafePhotoRepository;
import com.example.GoCafe.repository.CafeRepository;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.service.FileStorageService;
import com.example.GoCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cafes")
public class CafePhotoController {

    private final CafeRepository cafeRepository;
    private final MemberRepository memberRepository;
    private final CafePhotoRepository cafePhotoRepository;
    private final FileStorageService storage;

    // ====== 업로드 (점주만) ===================================================
    @PostMapping("/{cafeId}/photos")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CafePhoto>> upload(@PathVariable Long cafeId,
                                                  @RequestParam("files") List<MultipartFile> files,
                                                  @RequestParam(value = "setMain", defaultValue = "false") boolean setMain,
                                                  Authentication auth) throws Exception {
        Cafe cafe = cafeRepository.findById(cafeId).orElseThrow(() -> new NotFoundException("카페 없음"));
        ensureOwner(auth, cafe);

        // 기존 사진/대표 존재 여부
        List<CafePhoto> existing = cafePhotoRepository.findByCafe_Id(cafeId);
        boolean hasMain = existing.stream().anyMatch(p -> Boolean.TRUE.equals(p.getMain()));

        // sortIndex 기준 next 값
        int nextOrder = existing.stream()
                .map(CafePhoto::getSortIndex)
                .max(Comparator.nullsFirst(Integer::compareTo))
                .orElse(-1) + 1;

        boolean mainAssignedThisRound = false;

        for (MultipartFile f : files) {
            // 파일 저장 (FileStorageService 구현에 따라 메서드명이 save/store 다를 수 있음)
            // save(...)가 StoredFile(meta)를 돌려준다는 가정. 만약 String(url)만 준다면 그에 맞춰 아래 두 줄 수정.
            var sf = storage.store(f, "cafes/" + cafeId); // url(), originalName(), contentType(), sizeBytes()

            CafePhoto img = new CafePhoto();
            img.setCafe(cafe);
            img.setUrl(sf.url());
            img.setOriginalName(sf.originalName());
            img.setContentType(sf.contentType());
            img.setSizeBytes(sf.sizeBytes());
            img.setSortIndex(nextOrder++);

            // 대표 지정 규칙: (1) setMain=true면 이번 업로드 중 첫 장을 대표로, (2) 기존 대표가 없으면 첫 장을 대표로
            if ((!hasMain && !mainAssignedThisRound) || (setMain && !mainAssignedThisRound)) {
                // 기존 대표 해제
                cafePhotoRepository.findByCafe_IdAndMainTrue(cafeId).ifPresent(prev -> {
                    prev.setMain(false);
                    cafePhotoRepository.save(prev);
                });
                img.setMain(true);
                mainAssignedThisRound = true;
                hasMain = true;
            } else {
                img.setMain(false);
            }

            cafePhotoRepository.save(img);
        }

        // 업로드 후 정렬된 전체 목록 반환
        return ResponseEntity.ok(cafePhotoRepository.findByCafe_IdOrderBySortIndexAsc(cafeId));
    }

    // ====== 대표 설정 (점주만) ===============================================
    @PatchMapping("/photos/{photoId}/main")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CafePhoto>> setMain(@PathVariable Long photoId, Authentication auth) {
        CafePhoto target = cafePhotoRepository.findById(photoId)
                .orElseThrow(() -> new NotFoundException("사진 없음"));
        Cafe cafe = target.getCafe();
        ensureOwner(auth, cafe);

        // 기존 대표 해제
        cafePhotoRepository.findByCafe_IdAndMainTrue(cafe.getId()).ifPresent(prev -> {
            if (!prev.getId().equals(photoId)) {
                prev.setMain(false);
                cafePhotoRepository.save(prev);
            }
        });

        // 대상 대표 지정
        target.setMain(true);
        cafePhotoRepository.save(target);

        return ResponseEntity.ok(cafePhotoRepository.findByCafe_IdOrderBySortIndexAsc(cafe.getId()));
    }

    // ====== 목록 조회 (누구나) ================================================
    @GetMapping("/{cafeId}/photos")
    public List<CafePhoto> list(@PathVariable Long cafeId) {
        return cafePhotoRepository.findByCafe_IdOrderBySortIndexAsc(cafeId);
    }

    // ====== 삭제 (점주만) =====================================================
    @DeleteMapping("/photos/{photoId}")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CafePhoto>> delete(@PathVariable Long photoId, Authentication auth) {
        CafePhoto target = cafePhotoRepository.findById(photoId)
                .orElseThrow(() -> new NotFoundException("사진 없음"));
        Cafe cafe = target.getCafe();
        ensureOwner(auth, cafe);

        // 파일 실제 삭제 (구현돼 있으면)
        try { storage.delete(target.getUrl()); } catch (Throwable ignored) {}

        boolean wasMain = Boolean.TRUE.equals(target.getMain());
        cafePhotoRepository.delete(target);

        // 대표가 사라졌다면 첫 번째 사진을 대표로
        if (wasMain) {
            cafePhotoRepository.findByCafe_IdAndMainTrue(cafe.getId()).orElseGet(() -> {
                var list = cafePhotoRepository.findByCafe_IdOrderBySortIndexAsc(cafe.getId());
                if (!list.isEmpty()) {
                    CafePhoto first = list.get(0);
                    first.setMain(true);
                    cafePhotoRepository.save(first);
                }
                return null;
            });
        }

        return ResponseEntity.ok(cafePhotoRepository.findByCafe_IdOrderBySortIndexAsc(cafe.getId()));
    }

    // ====== 소유자/관리자 확인 ===============================================
    private void ensureOwner(Authentication auth, Cafe cafe) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("로그인이 필요합니다.");
        }

        String email = auth.getName();
        Member me = memberRepository.findByEmail(email).orElse(null);

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean isOwner = me != null && cafe.getOwner() != null
                && cafe.getOwner().getId().equals(me.getId());

        if (!(isOwner || isAdmin)) {
            throw new org.springframework.security.access.AccessDeniedException("점주만 가능합니다.");
        }
    }
}
