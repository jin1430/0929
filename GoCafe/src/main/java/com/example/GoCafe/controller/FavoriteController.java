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
        // JWT 필터에서 setName(email) 형태라고 하셨으니 email로 조회
        String email = authentication.getName();
        Member me = memberRepository.findByMemberEmail(email)
                .orElseThrow(() -> new IllegalStateException("로그인 사용자를 찾을 수 없습니다."));
        return me.getMemberId();
    }

    @PostMapping("/{cafeId}/toggle")
    public Map<String, Object> toggle(@PathVariable Long cafeId, Authentication auth) {
        boolean favorited = favoriteService.toggle(currentMemberId(auth), cafeId);
        long count = favoriteService.countForCafe(cafeId);
        return Map.of("favorited", favorited, "count", count);
    }

    @GetMapping
    public Page<Cafe> myFavorites(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "12") int size,
                                  Authentication auth) {
        return favoriteService.listMyFavorites(currentMemberId(auth), PageRequest.of(page, size));
    }

    @GetMapping("/cafes/{cafeId}/count")
    public Map<String, Long> count(@PathVariable Long cafeId) {
        return Map.of("count", favoriteService.countForCafe(cafeId));
    }
}
