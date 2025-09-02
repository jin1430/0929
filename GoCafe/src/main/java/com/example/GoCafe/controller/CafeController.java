// src/main/java/com/example/GoCafe/controller/CafeController.java
package com.example.GoCafe.controller;

import com.example.GoCafe.domain.CafeStatus;
import com.example.GoCafe.dto.CafeForm;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.service.CafeService;
import com.example.GoCafe.service.CafeStatsService;
import com.example.GoCafe.service.MemberService;
import com.example.GoCafe.service.ReviewService;
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

        Long cafeId = cafeService.createCafe(me.getMemberId(), form, cafePhotoFile, bizDocFile);

        ra.addFlashAttribute("msg", "카페가 등록되었습니다. (승인 대기 중)");
        return "redirect:/cafes/" + cafeId;
    }

    @GetMapping("/{cafeId}")
    public String viewCafe(@PathVariable Long cafeId,
                           Authentication auth,
                           Model model) {

        Cafe cafe = cafeService.findById(cafeId);

        String email = (auth != null ? auth.getName() : null);
        Long meId = null;
        boolean isAdmin = false;

        if (auth != null) {
            isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        }
        if (email != null) {
            Member me = memberService.findByEmail(email);
            meId = (me != null ? me.getMemberId() : null);
        }
        boolean isOwner = (meId != null && cafe.getCafeOwnerId() != null
                && meId.equals(cafe.getCafeOwnerId()));

        // 승인 상태가 아니면 소유자/관리자만 열람 허용
        if (cafe.getStatus() != CafeStatus.APPROVED && !(isOwner || isAdmin)) {
            throw new com.example.GoCafe.support.NotFoundException("승인되지 않은 카페입니다.");
        }

        model.addAttribute("cafe", cafe);
        model.addAttribute("reviews", reviewService.findByCafeIdWithMember(cafeId));

        // ✅ 좋아요/아쉬워요 합계 + LIKE 태그 상위 12개
        var stats = cafeStatsService.buildStats(cafeId, 12);
        model.addAttribute("cafeGood", stats.get("good"));
        model.addAttribute("cafeBad",  stats.get("bad"));
        model.addAttribute("cafeTags", stats.get("tags")); // 템플릿의 {{#cafeTags}}가 사용

        return "cafes/detail";
    }
}
