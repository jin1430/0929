// src/main/java/com/example/GoCafe/controller/AdminMissionController.java
package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Notification;
import com.example.GoCafe.entity.Review;
import com.example.GoCafe.repository.NotificationRepository;
import com.example.GoCafe.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/admin/missions")
@PreAuthorize("hasRole('ADMIN')") // 시큐리티 설정에 맞춰 조정
@RequiredArgsConstructor
public class AdminMissionController {

    private final NotificationRepository notificationRepository;
    private final ReviewRepository reviewRepository;

    @GetMapping("/completed")
    public String completed(Model model) {
        // 아주 단순: 전체 Notification에서 COMPLETED만 수집 (필요하면 페이지네이션 추가)
        List<Notification> all = notificationRepository.findAll();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Notification n : all) {
            if (n.getMessage() != null && n.getMessage().startsWith("MISSION:COMPLETED:")) {
                Long reviewId = Long.valueOf(n.getMessage().substring("MISSION:COMPLETED:".length()));
                Review r = reviewRepository.findById(reviewId).orElse(null);
                if (r == null) continue;
                rows.add(Map.of(
                        "reviewId", r.getId(),
                        "memberNickname", r.getMember().getNickname(),
                        "cafeName", r.getCafe().getName(),
                        "sentiment", r.getSentiment() // 현재 값 노출
                ));
            }
        }
        model.addAttribute("rows", rows);
        return "admin/missions/completed";
    }

    @PostMapping("/{reviewId}/sentiment")
    public String mark(@PathVariable Long reviewId, @RequestParam String v) {
        Review r = reviewRepository.findById(reviewId).orElseThrow();
        if (!"GOOD".equals(v) && !"BAD".equals(v)) {
            return "redirect:/admin/missions/completed?error=bad_param";
        }
        r.setSentiment(v);
        reviewRepository.save(r);
        return "redirect:/admin/missions/completed?ok=marked";
    }
}
