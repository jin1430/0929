package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Mission;
import com.example.GoCafe.entity.Notification;
import com.example.GoCafe.repository.MissionRepository;
import com.example.GoCafe.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/admin/missions/review")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMissionReviewController {

    private final NotificationRepository notificationRepository;
    private final MissionRepository missionRepository;

    // 제출 목록
    @GetMapping
    public String list(Model model) {
        var subs = notificationRepository.findAllMissionSubmissions();
        List<Map<String,Object>> view = new ArrayList<>();
        Pattern p = Pattern.compile("^MISSION:SUBMITTED:(\\d+)(?:\\|(.*))?$");
        for (Notification n : subs) {
            Matcher m = p.matcher(n.getMessage());
            if (!m.find()) continue;
            Long missionId = Long.valueOf(m.group(1));
            Mission mission = missionRepository.findById(missionId).orElse(null);

            String proofUrl = null, note = null;
            String rest = m.group(2);
            if (rest != null) {
                for (String kv : rest.split("\\|")) {
                    if (kv.startsWith("proofUrl=")) proofUrl = kv.substring(9);
                    else if (kv.startsWith("note=")) note = kv.substring(5);
                }
            }

            Map<String,Object> row = new LinkedHashMap<>();
            row.put("notifId", n.getId());
            row.put("memberEmail", n.getRecipient()!=null ? n.getRecipient().getEmail() : "-");
            row.put("missionId", missionId);
            row.put("missionTitle", mission!=null ? mission.getTitle() : "(삭제됨)");
            row.put("cafeId", mission!=null && mission.getCafe()!=null ? mission.getCafe().getId() : null);
            row.put("proofUrl", proofUrl);
            row.put("note", note);
            row.put("submittedAt", n.getCreatedAt());
            view.add(row);
        }
        model.addAttribute("submissions", view);
        return "admin/mission-review";
    }

    // 판정: GOOD → COMPLETED 로그 기록
    @PostMapping("/{notifId}/good")
    public String good(@PathVariable Long notifId, @RequestParam(required=false) String back) {
        Notification sub = notificationRepository.findById(notifId).orElseThrow();
        Long missionId = parseMissionId(sub.getMessage());
        Notification done = new Notification();
        done.setRecipient(sub.getRecipient());
        done.setCafe(sub.getCafe());
        done.setMessage("MISSION:COMPLETED:" + missionId + "|v=GOOD");
        done.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(done);
        return "redirect:" + (back == null ? "/admin/missions/review" : back);
    }

    // 판정: BAD → REJECTED 로그 기록 (사유 옵션)
    @PostMapping("/{notifId}/bad")
    public String bad(@PathVariable Long notifId,
                      @RequestParam(required=false) String reason,
                      @RequestParam(required=false) String back) {
        Notification sub = notificationRepository.findById(notifId).orElseThrow();
        Long missionId = parseMissionId(sub.getMessage());
        String msg = "MISSION:REJECTED:" + missionId + "|v=BAD";
        if (reason != null && !reason.isBlank()) msg += "|reason=" + reason.trim().replaceAll("\\s+"," ");
        Notification rej = new Notification();
        rej.setRecipient(sub.getRecipient());
        rej.setCafe(sub.getCafe());
        rej.setMessage(msg);
        rej.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(rej);
        return "redirect:" + (back == null ? "/admin/missions/review" : back);
    }

    private Long parseMissionId(String message) {
        Matcher m = Pattern.compile("^MISSION:(?:SUBMITTED|COMPLETED|REJECTED):(\\d+)").matcher(message);
        if (!m.find()) throw new IllegalArgumentException("잘못된 로그: " + message);
        return Long.valueOf(m.group(1));
    }
}
