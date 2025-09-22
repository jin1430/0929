package com.example.GoCafe.service;

import com.example.GoCafe.domain.ReportStatus;
import com.example.GoCafe.entity.*;
import com.example.GoCafe.repository.ReviewReportRepository;
import com.example.GoCafe.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewReportService {

    private final ReviewReportRepository reportRepo;
    private final ReviewRepository reviewRepo;

    // 신고 접수
    public ReviewReport reportReview(Long reviewId, Member reporter, String reason) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다: " + reviewId));

        ReviewReport report = ReviewReport.builder()
                .review(review)
                .reporter(reporter)
                .reason(reason)
                .status(ReportStatus.PENDING)
                .build();

        return reportRepo.save(report);
    }

    // 관리자: 미처리 신고 조회
    public List<ReviewReport> getPendingReports() {
        return reportRepo.findByStatus(ReportStatus.PENDING);
    }

    // 관리자: 신고 처리
    @Transactional
    public void handleReport(Long reportId, boolean approve) {
        ReviewReport report = reportRepo.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다: " + reportId));

        if (approve) {
            report.setStatus(ReportStatus.APPROVED);
            // 리뷰 삭제
            reviewRepo.delete(report.getReview());
        } else {
            report.setStatus(ReportStatus.REJECTED);
        }
    }
}
