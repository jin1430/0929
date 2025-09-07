package com.example.GoCafe.controller;

import com.example.GoCafe.entity.CafePhoto;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.repository.CafePhotoRepository;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FavoritePageController {

    private final FavoriteService favoriteService;
    private final MemberRepository memberRepository;
    private final CafePhotoRepository cafePhotoRepository; // CafePhoto 리포지토리

    @GetMapping("/favorites")
    public String favoritesPage(Authentication auth, Model model) {
        // 로그인 사용자 조회
        String email = auth.getName();
        Member loginMember = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("로그인 사용자를 찾을 수 없습니다."));

        var page = favoriteService.listMyFavorites(loginMember.getId(), PageRequest.of(0, 50));

        // 메인 사진: isMainPhoto=true 우선, 없으면 정렬값 가장 앞(originalName ASC)로 폴백
        List<CafeDto> favorites = page.stream().map(cafe -> {
            String mainUrl = cafePhotoRepository
                    .findFirstByCafe_IdAndIsMainTrueOrderBySortIndexAsc(cafe.getId())
                    .map(CafePhoto::getUrl)
                    .orElseGet(() -> cafePhotoRepository
                            .findFirstByCafe_IdOrderBySortIndexAsc(cafe.getId())
                            .map(CafePhoto::getUrl)
                            .orElse(null));
            return new CafeDto(cafe.getId(), cafe.getName(), cafe.getAddress(), mainUrl);
        }).toList();

        model.addAttribute("favorites", favorites);
        model.addAttribute("nav_favorites", true); // 네비 하이라이트
        return "member/favorites";
    }

    public record CafeDto(Long cafeId, String cafeName, String cafeAddress, String cafePhoto) {}
}