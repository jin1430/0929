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

    @Transactional(readOnly = true)
    public boolean isProEligible(Long memberId) {
        long total = safeCount(() -> reviewRepository.countByMember_Id(memberId));
        long good  = safeCount(() -> reviewRepository.countByMember_IdAndSentiment(memberId, "GOOD"));
        return total >= MIN_TOTAL && good >= MIN_GOOD;
    }

    @Transactional
    public void refreshRoleKind(Long memberId) {
        Member m = memberRepository.findById(memberId).orElseThrow();

        // ★ 관리자/점주는 절대 강등/변경하지 않음
        RoleKind current = m.getRoleKind();
        if (current == RoleKind.ADMIN || current == RoleKind.OWNER) {
            return; // 보안 권한은 그대로 유지
        }

        // 여기서부터는 등급만 (MEMBER <-> PRO) 스위칭
        boolean eligible = isProEligible(memberId);
        RoleKind target = eligible ? RoleKind.PRO : RoleKind.MEMBER;

        if (current != target) {
            m.setRoleKind(target);
            memberRepository.save(m);
        }
    }

    // Repo 네이티브 카운트가 null 가능/예외 가능할 때 방어
    private long safeCount(CountSupplier supplier) {
        try {
            Long v = supplier.get();
            return v == null ? 0L : v;
        } catch (Exception e) {
            return 0L;
        }
    }

    @FunctionalInterface
    private interface CountSupplier {
        Long get();
    }
}
