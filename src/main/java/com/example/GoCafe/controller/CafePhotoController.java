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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cafes")
public class CafePhotoController {

    private final CafeRepository cafeRepository;
    private final MemberRepository memberRepository;
    private final CafePhotoRepository cafePhotoRepository;
    private final FileStorageService storage;

    // ====== 업로드 (점주만) ===================================================
    @PostMapping("/{cafeId}/photos")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public String upload(@PathVariable Long cafeId,
                         @RequestParam("files") List<MultipartFile> files,
                         @RequestParam(value = "setMain", defaultValue = "false") boolean setMain,
                         Authentication auth,
                         RedirectAttributes ra) throws Exception {

        Cafe cafe = cafeRepository.findById(cafeId).orElseThrow(() -> new NotFoundException("카페 없음"));
        ensureOwner(auth, cafe);

        List<CafePhoto> existing = cafePhotoRepository.findByCafe_Id(cafeId);
        boolean hasMain = existing.stream().anyMatch(p -> Boolean.TRUE.equals(p.getIsMain()));

        int nextOrder = existing.stream()
                .map(CafePhoto::getSortIndex)
                .max(Comparator.nullsFirst(Integer::compareTo))
                .orElse(-1) + 1;

        boolean mainAssignedThisRound = false;

        for (MultipartFile f : files) {
            var sf = storage.store(f, "cafes/" + cafeId); // 실제 파일 저장

            CafePhoto img = new CafePhoto();
            img.setCafe(cafe);
            img.setUrl(sf.url());
            img.setOriginalName(sf.originalName());
            img.setContentType(sf.contentType());
            img.setSizeBytes(sf.sizeBytes());
            img.setSortIndex(nextOrder++);

            // 대표 지정 규칙
            if ((!hasMain && !mainAssignedThisRound) || (setMain && !mainAssignedThisRound)) {
                cafePhotoRepository.findByCafe_IdAndIsMainTrue(cafeId).ifPresent(prev -> {
                    prev.setIsMain(false);
                    cafePhotoRepository.save(prev);
                });
                img.setIsMain(true);
                mainAssignedThisRound = true;
                hasMain = true;
            } else {
                img.setIsMain(false);
            }

            cafePhotoRepository.save(img);
        }

        ra.addFlashAttribute("toast", "사진이 업로드되었습니다.");
        return "redirect:/cafes/" + cafeId; // ✅ 상세 페이지로 이동
    }

    // ====== 대표 설정 (점주만) ===============================================
    // HTML 폼 호환을 위해 PATCH 대신 POST 사용
    @PostMapping("/photos/{photoId}/main")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public String setMain(@PathVariable Long photoId,
                          Authentication auth,
                          RedirectAttributes ra) {

        CafePhoto target = cafePhotoRepository.findById(photoId)
                .orElseThrow(() -> new NotFoundException("사진 없음"));
        Cafe cafe = target.getCafe();
        ensureOwner(auth, cafe);

        cafePhotoRepository.findByCafe_IdAndIsMainTrue(cafe.getId()).ifPresent(prev -> {
            if (!prev.getId().equals(photoId)) {
                prev.setIsMain(false);
                cafePhotoRepository.save(prev);
            }
        });

        target.setIsMain(true);
        cafePhotoRepository.save(target);

        ra.addFlashAttribute("toast", "대표 사진이 변경되었습니다.");
        return "redirect:/cafes/" + cafe.getId(); // ✅ 상세로
    }

    // ====== 목록 조회 (뷰에 직접 바인딩하거나, AJAX가 필요하면 @ResponseBody로 전환) ======
    @GetMapping("/{cafeId}/photos")
    public String list(@PathVariable Long cafeId,
                       org.springframework.ui.Model model) {
        List<CafePhoto> photos = cafePhotoRepository.findByCafe_IdOrderBySortIndexAsc(cafeId);
        model.addAttribute("photos", photos);
        model.addAttribute("cafeId", cafeId);
        return "cafe/photos"; // 예: templates/cafe/photos.mustache
    }

    // ====== 삭제 (점주만) =====================================================
    // DELETE 대신 POST 사용(폼 호환)
    @PostMapping("/photos/{photoId}/delete")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public String delete(@PathVariable Long photoId,
                         Authentication auth,
                         RedirectAttributes ra) {

        CafePhoto target = cafePhotoRepository.findById(photoId)
                .orElseThrow(() -> new NotFoundException("사진 없음"));
        Cafe cafe = target.getCafe();
        ensureOwner(auth, cafe);

        try { storage.delete(target.getUrl()); } catch (Throwable ignored) {}

        boolean wasMain = Boolean.TRUE.equals(target.getIsMain());
        cafePhotoRepository.delete(target);

        if (wasMain) {
            cafePhotoRepository.findByCafe_IdAndIsMainTrue(cafe.getId()).orElseGet(() -> {
                var list = cafePhotoRepository.findByCafe_IdOrderBySortIndexAsc(cafe.getId());
                if (!list.isEmpty()) {
                    CafePhoto first = list.get(0);
                    first.setIsMain(true);
                    cafePhotoRepository.save(first);
                }
                return null;
            });
        }

        ra.addFlashAttribute("toast", "사진이 삭제되었습니다.");
        return "redirect:/cafes/" + cafe.getId(); // ✅ 상세로
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