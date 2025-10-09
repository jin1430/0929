// src/main/java/com/example/GoCafe/controller/AdminPageController.java
package com.example.GoCafe.controller;

import com.example.GoCafe.domain.CafeStatus;
import com.example.GoCafe.domain.ReportStatus;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.Notification;
import com.example.GoCafe.entity.Review;
import com.example.GoCafe.repository.*;
import com.example.GoCafe.service.CafeService;
import com.example.GoCafe.service.ProGateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPageController {

    private final CafeService cafeService;
    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final ProGateService proGateService;
    private final NotificationRepository notificationRepository;
    private final MissionRepository missionRepository;

    @GetMapping
    public String main(@RequestParam(defaultValue = "cafes") String tab, Model model) {
        // 상단 카드
        model.addAttribute("pendingCafeCount",  cafeService.countByStatus(CafeStatus.PENDING));
        model.addAttribute("approvedCafeCount", cafeService.countAll()); // "총 카페 수"는 전체 개수
        model.addAttribute("reviewTotalCount",  reviewRepository.count());
        model.addAttribute("activeUserCount",   memberRepository.count());
        model.addAttribute("reportPendingCount", reviewReportRepository.countByStatus(ReportStatus.PENDING));

        // 탭 플래그
        boolean isCafes    = "cafes".equalsIgnoreCase(tab);
        boolean isMissions = "missions".equalsIgnoreCase(tab);
        boolean isReports  = "reports".equalsIgnoreCase(tab);
        boolean isMembers  = "members".equalsIgnoreCase(tab);
        model.addAttribute("isTabCafes", isCafes);
        model.addAttribute("isTabMissions", isMissions);
        model.addAttribute("isTabReports", isReports);
        model.addAttribute("isTabMembers", isMembers);

        // 탭별 데이터
        if (isCafes) {
            model.addAttribute("pendingCafes", cafeService.findByStatus(CafeStatus.PENDING));
        }

        if (isMissions) {
            model.addAttribute("missionReviews", buildMissionRows());
        }

        if (isReports) {
            // 템플릿에서 id/reviewId/reason 사용
            model.addAttribute("pendingReports",
                    reviewReportRepository.findByStatus(ReportStatus.PENDING));
        }

        if (isMembers) {
            List<Map<String, Object>> candidates = memberRepository.findAll().stream()
                    .map(m -> Map.<String,Object>of(
                            "id", m.getId(),
                            "nickname", m.getNickname(),
                            "email", m.getEmail(),
                            "roleKind", String.valueOf(m.getRoleKind()),
                            "totalReviews", reviewRepository.countByMember_Id(m.getId()),
                            "goodReviews",  reviewRepository.countByMember_IdAndSentiment(m.getId(), "GOOD"),
                            "eligible",     proGateService.isProEligible(m.getId())
                    ))
                    .filter(m -> Boolean.TRUE.equals(m.get("eligible"))) // 후보만 노출
                    .toList();
            model.addAttribute("proCandidates", candidates);
            model.addAttribute("pendingMissionCount", missionRepository.findByActiveYnOrderByIdDesc("N").size());
        }

        return "admin/main";
    }

    /** AdminMissionController.completed() 로직 이식 */
    private List<Map<String, Object>> buildMissionRows() {
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
                        "currentSentiment", r.getSentiment()
                ));
            }
        }
        return rows;
    }
}
