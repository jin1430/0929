// src/main/java/com/example/GoCafe/controller/CafeController.java
package com.example.GoCafe.controller;

import com.example.GoCafe.domain.CafeStatus;
import com.example.GoCafe.dto.CafeForm;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.ReviewPhoto;
import com.example.GoCafe.repository.ReviewTagRepository;
import com.example.GoCafe.service.*;
import com.example.GoCafe.support.NotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cafes")
public class CafeController {

    private final CafeService cafeService;
    private final MemberService memberService;
    private final ReviewService reviewService;
    private final CafeStatsService cafeStatsService;
    private final ReviewPhotoService reviewPhotoService;
    private final ReviewTagRepository reviewTagRepository;
    private final FavoriteService favoriteService;
    // 🔹 더 이상 FavoriteRepository 직접 호출 안 함
    // private final FavoriteRepository favoriteRepository;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/new")
    public String createCafeForm() {
        return "cafes/create";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String createCafe(Authentication auth,
                             @Valid @ModelAttribute("form") CafeForm form,
                             @RequestParam(value = "cafePhotoFile", required = false) MultipartFile cafePhotoFile,
                             @RequestParam(value = "bizDocFile", required = false) MultipartFile bizDocFile,
                             RedirectAttributes ra) {

        if (auth == null || !auth.isAuthenticated()) {
            ra.addFlashAttribute("msg", "로그인이 필요한 서비스입니다.");
            return "redirect:/login";
        }

        String email = auth.getName();
        Member me = memberService.findByEmail(email);
        if (me == null) {
            ra.addFlashAttribute("msg", "로그인 정보를 찾을 수 없습니다.");
            return "redirect:/login";
        }

        Long cafeId = cafeService.createCafe(me.getId(), form, cafePhotoFile, bizDocFile);

        ra.addFlashAttribute("msg", "카페가 등록되었습니다. (승인 대기 중)");
        return "redirect:/cafes/" + cafeId;
    }

    @GetMapping("/{cafeId}")
    public String viewCafe(@PathVariable Long cafeId,
                           Authentication auth,
                           Model model) {

        // 1) 카페 조회
        Cafe cafe = cafeService.findById(cafeId);

        // 2) 로그인 사용자 정보/권한
        String email = (auth != null ? auth.getName() : null);
        Long meId = null;
        boolean isAdmin = false;

        if (auth != null) {
            isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        }
        if (email != null) {
            Member me = memberService.findByEmail(email);
            meId = (me != null ? me.getId() : null);
        }
        boolean isOwner = (meId != null && cafe.getOwner() != null
                && meId.equals(cafe.getOwner()));

        // 3) 승인 전 카페 접근 제한
        if (cafe.getStatus() != CafeStatus.APPROVED && !(isOwner || isAdmin)) {
            throw new NotFoundException("승인되지 않은 카페입니다.");
        }

        // 4) 모델—카페 본문
        model.addAttribute("cafe", cafe);

        // 5) 리뷰 목록 + 사진 주입
        var reviews = reviewService.findByCafeIdWithMember(cafeId);  // member는 fetch join 가정
        for (var r : reviews) {
            // 사진 엔티티 목록을 그대로 주입 (정렬은 sortIndex ASC)
            var photos = reviewPhotoService.findByReviewIdOrderBySortIndexAsc(r.getId());
            r.setPhotos(photos);
        }
        model.addAttribute("reviews", reviews);

        // 6) 좋아요/아쉬워요 + 태그 집계
        var stats = cafeStatsService.buildStats(cafeId, 12);
        model.addAttribute("cafeGood", stats.get("good"));
        model.addAttribute("cafeBad",  stats.get("bad"));
        model.addAttribute("cafeTags", stats.get("tags"));

        // 7) 즐겨찾기 상태/카운트
        boolean isFavorited = false;
        if (email != null) {
            isFavorited = favoriteService.isFavoritedByEmail(email, cafeId);
        }
        long favoriteCount = favoriteService.countForCafe(cafeId);
        model.addAttribute("isFavorited", isFavorited);
        model.addAttribute("favoriteCount", favoriteCount);

        return "cafes/detail";
    }

}
