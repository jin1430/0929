// src/main/java/com/example/GoCafe/controller/AdminMemberController.java
package com.example.GoCafe.controller;

import com.example.GoCafe.domain.RoleKind;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.service.ProGateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/members")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberRepository memberRepository;
    private final ProGateService proGateService;

    @PostMapping("/{memberId}/promote")
    public String promote(@PathVariable Long memberId, @RequestParam(defaultValue = "PRO") String to) {
        Member m = memberRepository.findById(memberId).orElseThrow();
        RoleKind target = RoleKind.valueOf(to);
        m.setRoleKind(target);
        memberRepository.save(m);
        return "redirect:/admin#tab-members";
    }

    @PostMapping("/{memberId}/demote")
    public String demote(@PathVariable Long memberId, @RequestParam(defaultValue = "MEMBER") String to) {
        Member m = memberRepository.findById(memberId).orElseThrow();
        RoleKind target = RoleKind.valueOf(to);
        m.setRoleKind(target);
        memberRepository.save(m);
        return "redirect:/admin#tab-members";
    }

    // 필요시 수동 재평가(규칙 기반 자동)
    @PostMapping("/{memberId}/refresh-role")
    public String refreshByRule(@PathVariable Long memberId) {
        proGateService.refreshRoleKind(memberId);
        return "redirect:/admin#tab-members";
    }
}
