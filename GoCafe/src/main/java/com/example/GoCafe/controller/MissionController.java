// src/main/java/com/example/GoCafe/controller/MissionController.java
package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.Mission;
import com.example.GoCafe.entity.Notification;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.repository.MissionRepository;
import com.example.GoCafe.repository.NotificationRepository;
import com.example.GoCafe.service.ProGateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
@RequestMapping("/missions")
public class MissionController {

    private final MissionRepository missionRepository;
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final ProGateService proGateService;

    // 진행 중 미션 목록
    @GetMapping
    public String list(Model model, Authentication auth) {
        // 1) 미션 목록 조회
        List<Mission> src = missionRepository
                .findByActiveYnAndDueDateGreaterThanEqualOrderByIdDesc("Y", LocalDate.now());

        // 2) 템플릿에 맞게 변환 (cafeId 등)
        List<Map<String, Object>> missions = src.stream()
                .map(this::toView)
                .toList();

        model.addAttribute("missions", missions);
        model.addAttribute("missionCount", missions.size());

        // 3) 로그인/권한 플래그
        boolean loggedIn = auth != null && auth.isAuthenticated()
                && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
        model.addAttribute("loggedIn", loggedIn);

        String roleKind = "GUEST";
        boolean isPro = false;
        if (loggedIn) {
            Member me = memberRepository.findByEmail(auth.getName()).orElse(null);
            if (me != null) {
                proGateService.refreshRoleKind(me.getId());
                roleKind = String.valueOf(me.getRoleKind());
                isPro = "PRO".equalsIgnoreCase(roleKind);
            }
        }
        model.addAttribute("roleKind", roleKind);
        model.addAttribute("isPro", isPro);

        return "missions/index";
    }

    // 상세
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication auth, Model model) {
        Mission m = missionRepository.findById(id).orElseThrow();
        model.addAttribute("mission", toView(m));

        boolean loggedIn = auth != null && auth.isAuthenticated()
                && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
        model.addAttribute("loggedIn", loggedIn);

        String roleKind = "GUEST";
        boolean isPro = false;
        boolean applied = false;

        if (loggedIn) {
            Member me = memberRepository.findByEmail(auth.getName()).orElse(null);
            if (me != null) {
                proGateService.refreshRoleKind(me.getId());
                roleKind = String.valueOf(me.getRoleKind());
                isPro = "PRO".equalsIgnoreCase(roleKind);

                applied = notificationRepository.latestMissionLog(
                                me.getId(),
                                m.getCafe() == null ? -1L : m.getCafe().getId())
                        .map(n -> isAcceptedOrCompleted(n, id))
                        .orElse(false);
            }
        }

        model.addAttribute("roleKind", roleKind);
        model.addAttribute("isPro", isPro);
        model.addAttribute("applied", applied);
        model.addAttribute("memberMissionId", id);
        return "missions/detail";
    }

    // 수락
    @PostMapping("/{id}/apply")
    public String apply(@PathVariable Long id, Authentication auth) {
        Member me = currentMember(auth);
        Mission m = missionRepository.findById(id).orElseThrow();
        Long cafeId = (m.getCafe() == null ? -1L : m.getCafe().getId());

        // 중복 수락 방지
        boolean already = notificationRepository.latestMissionLog(me.getId(), cafeId)
                .map(n -> isAcceptedOrCompleted(n, id))
                .orElse(false);
        if (!already) {
            Notification log = new Notification();
            log.setRecipient(me);
            log.setCafe(m.getCafe());
            log.setMessage("MISSION:ACCEPTED:" + id);
            log.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(log);
        }
        return "redirect:/missions/" + id;
    }

    // 증빙 제출 → 관리자 심사 대기
    @PostMapping("/{id}/proof")
    public String submitProof(@PathVariable Long id,
                              @RequestParam(required = false) String proofUrl,
                              @RequestParam(required = false) String note,
                              Authentication auth) {
        Member me = currentMember(auth);
        Mission m = missionRepository.findById(id).orElseThrow();

        Notification log = new Notification();
        log.setRecipient(me);
        log.setCafe(m.getCafe());
        StringBuilder msg = new StringBuilder("MISSION:SUBMITTED:").append(id);
        if (proofUrl != null && !proofUrl.isBlank()) msg.append("|proofUrl=").append(proofUrl.trim());
        if (note != null && !note.isBlank()) msg.append("|note=").append(note.trim().replaceAll("\\s+", " "));
        log.setMessage(msg.toString());
        log.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(log);

        return "redirect:/missions/my";
    }

    // 내 미션
    @GetMapping("/my")
    public String my(Authentication auth, Model model) {
        Member me = currentMember(auth);
        List<Notification> all = notificationRepository.findByMemberCafeAndPrefix(me.getId(), -1L, "MISSION:");

        Map<Long, Map<String, Object>> byMission = new LinkedHashMap<>();
        Pattern pId = Pattern.compile("^MISSION:(ACCEPTED|SUBMITTED|COMPLETED|REJECTED):(\\d+)");
        for (Notification n : all) {
            Matcher m = pId.matcher(n.getMessage());
            if (!m.find()) continue;
            String type = m.group(1);
            Long missionId = Long.valueOf(m.group(2));
            byMission.putIfAbsent(missionId, new LinkedHashMap<>(Map.of(
                    "status", "NEW",
                    "acceptedAt", null,
                    "completedAt", null,
                    "proofUrl", null,
                    "note", null
            )));
            Map<String, Object> v = byMission.get(missionId);
            switch (type) {
                case "ACCEPTED" -> {
                    v.put("status", "ACCEPTED");
                    v.put("acceptedAt", n.getCreatedAt());
                }
                case "SUBMITTED" -> {
                    v.put("status", "SUBMITTED");
                    // proofUrl|note 파싱
                    String[] parts = n.getMessage().split("\\|");
                    for (String s : parts) {
                        if (s.startsWith("proofUrl=")) v.put("proofUrl", s.substring(9));
                        else if (s.startsWith("note=")) v.put("note", s.substring(5));
                    }
                }
                case "COMPLETED" -> {
                    v.put("status", "COMPLETED"); // GOOD
                    v.put("completedAt", n.getCreatedAt());
                }
                case "REJECTED" -> {
                    v.put("status", "REJECTED"); // BAD
                    v.put("completedAt", n.getCreatedAt());
                }
            }
        }

        List<Map<String, Object>> view = new ArrayList<>(byMission.values());
        model.addAttribute("myMissions", view);
        return "missions/my";
    }

    // ===== helpers =====
    private Member currentMember(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) throw new RuntimeException("로그인이 필요합니다.");
        Member me = memberRepository.findByEmail(auth.getName()).orElse(null);
        if (me == null) throw new RuntimeException("회원 정보를 찾을 수 없습니다.");
        proGateService.refreshRoleKind(me.getId());
        return me;
    }

    private boolean isAcceptedOrCompleted(Notification n, Long missionId) {
        String msg = n.getMessage();
        return msg.equals("MISSION:ACCEPTED:" + missionId) || msg.equals("MISSION:COMPLETED:" + missionId);
    }

    private Map<String, Object> toView(Mission m) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", m.getId());
        v.put("title", m.getTitle());
        v.put("description", m.getDescription());
        v.put("dueDate", m.getDueDate());
        v.put("sponsoredYn", m.getSponsoredYn());
        v.put("activeYn", m.getActiveYn());
        v.put("cafeId", m.getCafe() == null ? null : m.getCafe().getId());
        return v;
    }
}
