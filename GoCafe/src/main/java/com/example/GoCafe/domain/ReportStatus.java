package com.example.GoCafe.domain;

public enum ReportStatus {
    PENDING,   // 관리자 확인 전
    APPROVED,  // 신고 승인 → 리뷰 삭제
    REJECTED   // 신고 반려
}
