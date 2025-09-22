package com.example.GoCafe.repository;

import com.example.GoCafe.domain.ReportStatus;
import com.example.GoCafe.entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {
    List<ReviewReport> findByStatus(ReportStatus status);
    long countByStatus(ReportStatus status);
}
