// src/main/java/com/example/GoCafe/service/ProGateService.java
package com.example.GoCafe.service;

import com.example.GoCafe.domain.RoleKind;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProGateService {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;

    // 심플 룰: 총 리뷰 10건 이상 & GOOD 6건 이상이면 PRO
    private static final int MIN_TOTAL = 10;
    private static final int MIN_GOOD  = 6;

    public boolean isProEligible(Long memberId) {
        long total = reviewRepository.countByMember_Id(memberId);
        long good  = reviewRepository.countByMember_IdAndSentiment(memberId, "GOOD");
        return total >= MIN_TOTAL && good >= MIN_GOOD;
    }

    @Transactional
    public void refreshRoleKind(Long memberId) {
        Member m = memberRepository.findById(memberId).orElseThrow();
        boolean eligible = isProEligible(memberId);
        String target = eligible ? "PRO" : "MEMBER";
        if (!target.equals(m.getRoleKind())) {
            m.setRoleKind(RoleKind.valueOf(target));
            memberRepository.save(m);
        }
    }
}
