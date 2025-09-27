package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
@PreAuthorize("isAuthenticated()")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final MemberRepository memberRepository;

    private Long currentMemberId(Authentication authentication) {
        String email = authentication.getName();
        Member loginMember = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("로그인 사용자를 찾을 수 없습니다."));
        return loginMember.getId();
    }

    @PostMapping("/{cafeId}/favorite")
    public Map<String, Object> toggle(@PathVariable Long cafeId, Authentication auth) {
        // JWT의 subject(email) 사용
        Member me = memberRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("member not found"));
        boolean favorited = favoriteService.toggle(me.getId(), cafeId);
        long count = favoriteService.countByCafe(cafeId);
        return Map.of("favorited", favorited, "favoriteCount", count);
    }

    @GetMapping
    public Page<Cafe> myFavorites(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "12") int size,
                                  Authentication auth) {
        return favoriteService.listMyFavorites(currentMemberId(auth), PageRequest.of(page, size));
    }

    @GetMapping("/cafes/{cafeId}/count")
    public Map<String, Long> countFavorited(@PathVariable Long cafeId) {
        return Map.of("count", favoriteService.countFavoriteForCafe(cafeId));
    }
}
