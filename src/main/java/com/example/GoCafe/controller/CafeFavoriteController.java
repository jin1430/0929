// src/main/java/com/example/GoCafe/controller/CafeFavoriteController.java
package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Favorite;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.repository.CafeRepository;
import com.example.GoCafe.repository.FavoriteRepository;
import com.example.GoCafe.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CafeFavoriteController {

    private final FavoriteRepository favoriteRepository;
    private final CafeRepository cafeRepository;
    private final MemberService memberService;

    /** 폼(submit)에서 오는 토글 – 완료 후 원래 페이지로 리다이렉트 */
    @PostMapping("/cafes/{cafeId}/favorite/toggle")
    public String toggleFavoriteRedirect(@PathVariable Long cafeId,
                                         @AuthenticationPrincipal User principal,
                                         @RequestHeader(value = "Referer", required = false) String referer) {
        doToggle(cafeId, principal);
        return "redirect:" + (referer != null ? referer : "/cafes/" + cafeId);
    }

    /** fetch/AJAX용 – JSON 응답 */
    @PostMapping(value = "/api/cafes/{cafeId}/favorite/toggle")
    @ResponseBody
    public ResponseEntity<?> toggleFavoriteApi(@PathVariable Long cafeId,
                                               @AuthenticationPrincipal User principal) {
        boolean added = doToggle(cafeId, principal);
        long count = favoriteRepository.countByCafe_Id(cafeId);

        Map<String, Object> body = new HashMap<>();
        body.put("status", added ? "ADDED" : "REMOVED");
        body.put("count", count);
        return ResponseEntity.ok(body);
    }

    /** 실제 토글 로직 */
    private boolean doToggle(Long cafeId, User principal) {
        if (principal == null) throw new RuntimeException("로그인이 필요합니다.");
        Member me = memberService.findByEmail(principal.getUsername());
        Cafe cafe = cafeRepository.findById(cafeId).orElseThrow();

        var existing = favoriteRepository.findByMember_IdAndCafe_Id(me.getId(), cafeId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false; // removed
        } else {
            Favorite f = new Favorite();
            f.setMember(me);
            f.setCafe(cafe);
            try { Favorite.class.getMethod("setCreatedAt", LocalDateTime.class).invoke(f, LocalDateTime.now()); } catch (Exception ignore) {}
            favoriteRepository.save(f);
            return true; // added
        }
    }
}
