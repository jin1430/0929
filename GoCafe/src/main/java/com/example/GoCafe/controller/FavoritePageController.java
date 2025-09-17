// src/main/java/com/example/GoCafe/controller/FavoritePageController.java
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FavoritePageController {

    private final FavoriteService favoriteService;
    private final MemberRepository memberRepository;
    private final CafePhotoRepository cafePhotoRepository;

    @GetMapping("/favorites")
    public String favoritesPage(Authentication auth, Model model) {
        // 로그인 사용자
        String email = auth.getName();
        Member loginMember = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("로그인 사용자를 찾을 수 없습니다."));

        // 내가 찜한 카페들
        var page = favoriteService.listMyFavorites(loginMember.getId(), PageRequest.of(0, 50));

        // 배치로 각 카페의 "대표 우선, 없으면 sortIndex 빠른 순" 사진 1장씩 매핑
        List<Long> cafeIds = page.stream().map(c -> c.getId()).toList();

        // Repository에 아래 JPQL이 있어야 합니다:
        // @Query("select p from CafePhoto p where p.cafe.id in :cafeIds order by p.cafe.id asc, p.main desc, p.sortIndex asc")
        // List<CafePhoto> findForCafeIdsOrderByMainThenSort(@Param("cafeIds") Collection<Long> cafeIds);
        Map<Long, String> mainUrlByCafeId = new LinkedHashMap<>();
        if (!cafeIds.isEmpty()) {
            List<CafePhoto> ordered = cafePhotoRepository.findForCafeIdsOrderByMainThenSort(cafeIds);
            for (CafePhoto p : ordered) {
                Long cid = p.getCafe().getId();
                // 첫 번째(= main 우선, 그다음 sortIndex asc)가 들어가면 유지
                mainUrlByCafeId.putIfAbsent(cid, p.getUrl());
            }
        }

        // 뷰 모델
        List<CafeDto> favorites = page.stream()
                .map(cafe -> new CafeDto(
                        cafe.getId(),
                        cafe.getName(),
                        cafe.getAddress(),
                        mainUrlByCafeId.get(cafe.getId()) /* 없으면 null */
                ))
                .toList();

        model.addAttribute("favorites", favorites);
        model.addAttribute("nav_favorites", true);
        return "member/favorites";
    }

    public record CafeDto(Long cafeId, String cafeName, String cafeAddress, String cafePhoto) {}
}
