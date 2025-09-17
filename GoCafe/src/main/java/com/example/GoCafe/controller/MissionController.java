// src/main/java/com/example/GoCafe/controller/MissionController.java
package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafeInfo;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.Notification;
import com.example.GoCafe.repository.CafeInfoRepository;
import com.example.GoCafe.repository.CafeRepository;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.repository.NotificationRepository;
import com.example.GoCafe.service.ProGateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/missions")
public class MissionController {

    private final CafeInfoRepository cafeInfoRepository;
    private final CafeRepository cafeRepository;
    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final ProGateService proGateService;

    private static final String MARK_MISSION = "[미션]";
    private static final String MARK_SPONSORED = "[협찬]";

    @GetMapping
    public String list(Model model, Authentication auth) {
        boolean loggedIn = auth != null;
        String roleKind = "GUEST";
        boolean isPro = false;

        if (loggedIn) {
            Member me = memberRepository.findByEmail(auth.getName()).orElseThrow();
            // 최신 등급 반영
            proGateService.refreshRoleKind(me.getId());
            roleKind = me.getRoleKind();
            isPro = "PRO".equals(roleKind);
        }

        List<Map<String, Object>> cards = new ArrayList<>();
        for (CafeInfo ci : cafeInfoRepository.findByNoticeMarker(MARK_MISSION)) {
            Cafe cafe = ci.getCafe();
            String notice = Optional.ofNullable(ci.getNotice()).orElse("");
            boolean sponsored = notice.contains(MARK_SPONSORED);
            LocalDate due = parseDue(notice);
            cards.add(Map.of(
                    "cafeId", cafe.getId(),
                    "cafeName", cafe.getName(),
                    "summary", shorten(notice, 120),
                    "due", due != null ? due.toString() : "-",
                    "sponsored", sponsored
            ));
        }

        model.addAttribute("missions", cards);
        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("roleKind", roleKind);
        model.addAttribute("isPro", isPro);
        return "missions/list";
    }

    @GetMapping("/{cafeId}")
    public String detail(@PathVariable Long cafeId, Model model, Authentication auth) {
        Cafe cafe = cafeRepository.findById(cafeId).orElseThrow();
        String notice = cafeInfoRepository.findByCafe_Id(cafeId)
                .map(CafeInfo::getNotice).orElse("");

        boolean loggedIn = auth != null;
        String roleKind = "GUEST";
        boolean isPro = false;
        boolean accepted = false;
        boolean sponsored = notice.contains(MARK_SPONSORED);

        if (loggedIn) {
            Member me = memberRepository.findByEmail(auth.getName()).orElseThrow();
            proGateService.refreshRoleKind(me.getId());
            roleKind = me.getRoleKind();
            isPro = "PRO".equals(roleKind);
            accepted = notificationRepository.latestMissionLog(me.getId(), cafeId)
                    .map(n -> n.getMessage().startsWith("MISSION:ACCEPTED")
                            || n.getMessage().startsWith("MISSION:COMPLETED"))
                    .orElse(false);
        }

        model.addAttribute("cafeId", cafe.getId());
        model.addAttribute("cafeName", cafe.getName());
        model.addAttribute("notice", notice);
        model.addAttribute("due", parseDue(notice));
        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("roleKind", roleKind);
        model.addAttribute("isPro", isPro);
        model.addAttribute("accepted", accepted);
        model.addAttribute("sponsored", sponsored);
        return "missions/detail";
    }

    @PostMapping("/{cafeId}/accept")
    @PreAuthorize("isAuthenticated()")
    public String accept(@PathVariable Long cafeId, Authentication auth) {
        Member me = memberRepository.findByEmail(auth.getName()).orElseThrow();
        String notice = cafeInfoRepository.findByCafe_Id(cafeId).map(CafeInfo::getNotice).orElse("");
        boolean sponsored = notice != null && notice.contains(MARK_SPONSORED);

        // 협찬이면 PRO만 수락 가능
        if (sponsored) {
            proGateService.refreshRoleKind(me.getId());
            if (!"PRO".equals(me.getRoleKind())) {
                return "redirect:/missions/" + cafeId + "?error=pro_only";
            }
        }

        // 이미 수락한 기록이 있으면 재수락 금지
        boolean already = notificationRepository.latestMissionLog(me.getId(), cafeId)
                .map(n -> n.getMessage().startsWith("MISSION:ACCEPTED")
                        || n.getMessage().startsWith("MISSION:COMPLETED"))
                .orElse(false);
        if (already) return "redirect:/missions/" + cafeId + "?ok=already";

        Notification log = new Notification();
        log.setRecipient(me);
        log.setCafe(cafeRepository.findById(cafeId).orElseThrow());
        log.setMessage("MISSION:ACCEPTED");
        log.setRead(true); // 알람용이 아니므로 true로
        // type은 기존 enum 재사용 (없어도 필수 아님)
        // log.setType(NotificationType.REVIEW);
        notificationRepository.save(log);

        return "redirect:/missions/" + cafeId + "?ok=accepted";
    }

    private static LocalDate parseDue(String s) {
        if (s == null) return null;
        Matcher m = Pattern.compile("DUE:(\\d{4}-\\d{2}-\\d{2})").matcher(s);
        return m.find() ? LocalDate.parse(m.group(1)) : null;
    }

    private static String shorten(String s, int n) {
        if (s == null) return "";
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() > n ? t.substring(0, n) + "…" : t;
    }
}
