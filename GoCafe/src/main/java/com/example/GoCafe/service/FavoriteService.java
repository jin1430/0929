package com.example.GoCafe.service;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Favorite;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.repository.CafeRepository;
import com.example.GoCafe.repository.FavoriteRepository;
import com.example.GoCafe.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    private final CafeRepository cafeRepository;

    public boolean toggle(Long memberId, Long cafeId) {
        Member m = memberRepository.getReferenceById(memberId);
        Cafe c = cafeRepository.getReferenceById(cafeId);

        return favoriteRepository.findByMemberAndCafe(m, c)
                .map(f -> { favoriteRepository.delete(f); return false; })
                .orElseGet(() -> { favoriteRepository.save(Favorite.builder().member(m).cafe(c).build()); return true; });
    }

    @Transactional(readOnly = true)
    public long countForCafe(Long cafeId) {
        Cafe c = cafeRepository.getReferenceById(cafeId);
        return favoriteRepository.countByCafe(c);
    }

    @Transactional(readOnly = true)
    public Page<Cafe> listMyFavorites(Long memberId, Pageable pageable) {
        Member m = memberRepository.getReferenceById(memberId);
        return favoriteRepository.findByMember(m, pageable).map(Favorite::getCafe);
    }

    @Transactional(readOnly = true)
    public boolean isFavoritedByMemberId(Long memberId, Long cafeId) {
        Member m = memberRepository.getReferenceById(memberId);
        Cafe c = cafeRepository.getReferenceById(cafeId);
        return favoriteRepository.existsByMemberAndCafe(m, c);
    }

    @Transactional(readOnly = true)
    public boolean isFavoritedByEmail(String memberEmail, Long cafeId) {
        return memberRepository.findByMemberEmail(memberEmail)
                .map(me -> {
                    Cafe c = cafeRepository.getReferenceById(cafeId);
                    return favoriteRepository.existsByMemberAndCafe(me, c);
                })
                .orElse(false);
    }
}
