package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.Mission;
import com.example.GoCafe.repository.CafeRepository;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.repository.MissionRepository;
import com.example.GoCafe.service.ProGateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cafes/{cafeId}/missions")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class OwnerMissionController {

    private final MissionRepository missionRepository;
    private final CafeRepository cafeRepository;
    private final MemberRepository memberRepository;
    private final ProGateService proGateService;

    @PostMapping
    public String createForCafe(@PathVariable Long cafeId,
                                @RequestParam String title,
                                @RequestParam(required = false, defaultValue = "") String description,
                                @RequestParam String dueDate,
                                Authentication auth) {

        Member me = memberRepository.findByEmail(auth.getName()).orElseThrow();
        proGateService.refreshRoleKind(me.getId());
        Cafe cafe = cafeRepository.findById(cafeId).orElseThrow();
        // TODO: 실제 소유 검증 로직 있으면 추가

        Mission m = new Mission();
        m.setCafe(cafe);
        m.setTitle(title);
        m.setDescription(description);
        m.setDueDate(LocalDate.parse(dueDate));
        m.setSponsoredYn("N");
        m.setActiveYn("N"); // 승인 대기
        missionRepository.save(m);

        return "redirect:/cafes/" + cafeId + "?missionSubmitted=1";
    }
}
