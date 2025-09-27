package com.example.GoCafe.controller;

import com.example.GoCafe.domain.ReportStatus;
import com.example.GoCafe.entity.ReviewReport;
import com.example.GoCafe.repository.ReviewReportRepository;
import com.example.GoCafe.service.ReviewReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportPageController {

    private final ReviewReportService reportService;
    private final ReviewReportRepository reportRepo;

    @GetMapping
    public String list(Model model) {
        List<ReviewReport> pending = reportService.getPendingReports();
        model.addAttribute("reports", pending);
        model.addAttribute("pendingCount", reportRepo.countByStatus(ReportStatus.PENDING));
        return "admin/reports";
    }

    @PostMapping("/{reportId}/approve")
    public String approve(@PathVariable Long reportId) {
        reportService.handleReport(reportId, true);
        return "redirect:/admin/reports";
    }

    @PostMapping("/{reportId}/reject")
    public String reject(@PathVariable Long reportId) {
        reportService.handleReport(reportId, false);
        return "redirect:/admin/reports";
    }
}
