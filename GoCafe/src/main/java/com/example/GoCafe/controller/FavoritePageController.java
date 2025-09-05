package com.example.GoCafe.controller;// package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Member;
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

    @GetMapping("/favorites")
    public String favoritesPage(Authentication auth, Model model) {
        String email = auth.getName();
        Member me = memberRepository.findByMemberEmail(email)
                .orElseThrow(() -> new IllegalStateException("로그인 사용자를 찾을 수 없습니다."));

        var page = favoriteService.listMyFavorites(me.getMemberId(), PageRequest.of(0, 50));
        // 간단 DTO 변환(여기에 Cafe 엔티티 필드명에 맞춰 주세요)
        List<CafeDto> favorites = page.map(c -> new CafeDto(c.getCafeId(), c.getCafeName(), c.getCafeAddress(), c.getCafePhoto())).getContent();

        model.addAttribute("favorites", favorites);
        model.addAttribute("nav_favorites", true); // 네비 하이라이트 용
        return "member/favorites";
    }


    public record CafeDto(Long cafeId, String cafeName, String cafeAddress, String cafePhoto) {}
}
