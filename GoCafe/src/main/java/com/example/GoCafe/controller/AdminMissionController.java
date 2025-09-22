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
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMissionController {

    private final NotificationRepository notificationRepository;
    private final ReviewRepository reviewRepository;

    @GetMapping("/completed")
    public String completed(Model model) {
        List<Notification> all = notificationRepository.findAll();
        List<Map<String, Object>> missionReviews = new ArrayList<>();

        for (Notification n : all) {
            if (n.getMessage() != null && n.getMessage().startsWith("MISSION:COMPLETED:")) {
                Long reviewId = Long.valueOf(n.getMessage().substring("MISSION:COMPLETED:".length()));
                Review r = reviewRepository.findById(reviewId).orElse(null);
                if (r == null) continue;
                missionReviews.add(Map.of(
                        "reviewId", r.getId(),
                        "memberNickname", r.getMember()!=null ? r.getMember().getNickname() : "알수없음",
                        "cafeName", r.getCafe()!=null ? r.getCafe().getName() : "알수없음",
                        "currentSentiment", r.getSentiment()==null? "" : r.getSentiment()
                ));
            }
        }
        model.addAttribute("missionReviews", missionReviews);
        return "admin/main"; // 같은 페이지에서 탭으로 보여줄 때
        // 별도 페이지면 "admin/missions/completed"로 유지
    }

    @PostMapping("/{reviewId}/sentiment")
    public String mark(@PathVariable Long reviewId, @RequestParam String v) {
        if (!"GOOD".equals(v) && !"BAD".equals(v)) {
            return "redirect:/admin#tab-missions";
        }
        Review r = reviewRepository.findById(reviewId).orElseThrow();
        r.setSentiment(v);
        reviewRepository.save(r);
        return "redirect:/admin#tab-missions";
    }
}
