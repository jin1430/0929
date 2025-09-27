package com.example.GoCafe.apiController;

import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.ReviewReport;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.service.ReviewReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReviewReportController {

    private final ReviewReportService reportService;
    private final MemberRepository memberRepository;

    // 유저: 리뷰 신고
    @PostMapping("/{reviewId}")
    public ResponseEntity<?> reportReview(@PathVariable Long reviewId,
                                          @RequestParam String reason,
                                          Authentication auth) {
        Member me = memberRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("로그인 사용자를 찾을 수 없습니다."));

        ReviewReport report = reportService.reportReview(reviewId, me, reason);
        return ResponseEntity.ok(Map.of(
                "message", "신고가 접수되었습니다.",
                "reportId", report.getId()
        ));
    }

    // 관리자: 미처리 신고 조회
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ReviewReport> getPendingReports() {
        return reportService.getPendingReports();
    }

    // 관리자: 신고 처리 (approve=true 승인/삭제, false 반려)
    @PostMapping("/{reportId}/handle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> handleReport(@PathVariable Long reportId,
                                          @RequestParam boolean approve) {
        reportService.handleReport(reportId, approve);
        return ResponseEntity.ok(Map.of("message", "처리 완료", "approved", approve));
    }
}
