// src/main/java/com/example/GoCafe/controller/AdminMissionController.java
package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Mission;
import com.example.GoCafe.entity.Notification;
import com.example.GoCafe.entity.Review;
import com.example.GoCafe.repository.CafeRepository;
import com.example.GoCafe.repository.MissionRepository;
import com.example.GoCafe.repository.NotificationRepository;
import com.example.GoCafe.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/admin/missions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMissionController {

    private final NotificationRepository notificationRepository;
    private final ReviewRepository reviewRepository;
    private final MissionRepository missionRepository;
    private final CafeRepository cafeRepository;

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
    @GetMapping
    public String page(Model model) {
        List<Mission> pending = missionRepository.findByActiveYnOrderByIdDesc("N");
        model.addAttribute("pending", pending);
        model.addAttribute("cafes", cafeRepository.findAll());
        return "admin/missions";
    }
    // 관리자가 직접 생성(바로 활성화)
    @PostMapping
    public String create(@RequestParam(required = false) Long cafeId,
                         @RequestParam String title,
                         @RequestParam(required = false, defaultValue = "") String description,
                         @RequestParam String dueDate,
                         @RequestParam(required = false, defaultValue = "N") String sponsoredYn) {
        Mission m = new Mission();
        m.setTitle(title);
        m.setDescription(description);
        m.setDueDate(LocalDate.parse(dueDate));
        m.setActiveYn("Y");
        m.setSponsoredYn("N".equalsIgnoreCase(sponsoredYn) ? "N" : "Y");
        if (cafeId != null) {
            Cafe cafe = cafeRepository.findById(cafeId).orElseThrow();
            m.setCafe(cafe);
        }
        missionRepository.save(m);
        return "redirect:/admin/missions";
    }

    // 점주가 등록한 대기 미션 승인
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id) {
        Mission m = missionRepository.findById(id).orElseThrow();
        m.setActiveYn("Y");
        missionRepository.save(m);
        return "redirect:/admin/missions";
    }

    // 반려(간단히 삭제)
    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id) {
        missionRepository.deleteById(id);
        return "redirect:/admin/missions";
    }

    // 종료
    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id) {
        Mission m = missionRepository.findById(id).orElseThrow();
        m.setActiveYn("N");
        missionRepository.save(m);
        return "redirect:/admin/missions";
    }
}
