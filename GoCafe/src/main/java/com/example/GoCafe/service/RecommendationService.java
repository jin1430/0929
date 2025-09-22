package com.example.GoCafe.service;

import com.example.GoCafe.dto.RecommendedCafeDto;
import com.example.GoCafe.repository.RecommendationQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationQueryRepository repo;

    // 가중치 (application.yml로 뺄 수 있음)
    private final double W_TAG     = 0.55;  // 태그 유사도
    private final double W_RATING  = 0.20;  // 별점
    private final double W_POP     = 0.15;  // 리뷰수
    private final double W_RECENCY = 0.10;  // 최근성

    /** 사용자별 추천 (콜드스타트 시 전역 랭킹 반환) */
    public List<RecommendedCafeDto> recommendForUser(Long memberId, int topK) {
        // 1) 후보군 로드 (전체 중 상위 N개만 점수 계산 → 성능 보호)
        List<Long> candidate = repo.loadCandidateCafeIds(2000);
        if (candidate.isEmpty()) return List.of();

        // 2) 요약/태그 로딩
        var summaries = repo.loadCafeSummaries(candidate);
        var cafeTags  = repo.loadAllCafeTags();
        Map<Long, Map<String, Double>> cafeVec = cafeTags;

        // 3) 사용자 선호 태그
        Map<String, Double> userPref = memberId != null ? repo.loadUserPreferenceTags(memberId, 200) : Map.of();

        // 4) 정규화용 기준 계산
        double maxReviews = summaries.stream().map(s -> s.reviewCount != null ? s.reviewCount : 0)
                .mapToDouble(Integer::doubleValue).max().orElse(1);
        double maxDays = 0;
        LocalDateTime now = LocalDateTime.now();
        for (var s : summaries) {
            if (s.lastReviewAt == null) continue;
            double days = Math.max(0, Duration.between(s.lastReviewAt, now).toDays());
            if (days > maxDays) maxDays = days;
        }
        if (maxDays == 0) maxDays = 1;

        // 5) 벡터 정규화 (cosine 유사도용)
        Map<String, Double> userUnit = unit(userPref);
        // 6) 점수 계산
        List<RecommendedCafeDto> out = new ArrayList<>();
        for (var s : summaries) {
            Map<String, Double> cvec = cafeVec.getOrDefault(s.cafeId, Map.of());
            double tagScore = cosine(userUnit, unit(cvec)); // 0~1 근사

            double ratingScore = 0;
            if (s.avgRating != null) {
                // 별점 0~5 가정 → 0~1 스케일
                ratingScore = Math.max(0, Math.min(1, s.avgRating / 5.0));
            }

            double popularity = Math.min(1.0, (s.reviewCount != null ? s.reviewCount : 0) / Math.max(1.0, maxReviews));

            double recency = 0.5; // 기본값
            if (s.lastReviewAt != null) {
                double days = Math.max(0, Duration.between(s.lastReviewAt, now).toDays());
                // 최근일수 적을수록 높은 점수: 1 - (days / maxDays)
                recency = Math.max(0, 1.0 - (days / maxDays));
            }

            // 콜드스타트: userPref가 없으면 태그 가중치 낮춰 전체 품질 중심
            double wTag = (userPref.isEmpty() ? 0.20 : W_TAG);
            double score = wTag * tagScore + W_RATING * ratingScore + W_POP * popularity + W_RECENCY * recency;

            RecommendedCafeDto dto = RecommendedCafeDto.builder()
                    .cafeId(s.cafeId)
                    .name(s.name)
                    .address(s.address)
                    .mainPhotoUrl(s.mainPhotoUrl)
                    .avgRating(s.avgRating)
                    .reviewCount(s.reviewCount)
                    .tagScore(tagScore)
                    .ratingScore(ratingScore)
                    .popularity(popularity)
                    .recency(recency)
                    .score(score)
                    .build();
            out.add(dto);
        }

        // 7) 정렬 후 topK
        return out.stream()
                .sorted(Comparator.comparingDouble(RecommendedCafeDto::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    /** 특정 카페와 유사한 카페 추천 */
    public List<RecommendedCafeDto> similarCafes(Long cafeId, int topK) {
        var cafeTags = repo.loadAllCafeTags();
        Map<String, Double> base = cafeTags.getOrDefault(cafeId, Map.of());
        if (base.isEmpty()) return List.of();
        Map<String, Double> baseUnit = unit(base);

        var candidateIds = new ArrayList<>(cafeTags.keySet());
        candidateIds.remove(cafeId);
        var summaries = repo.loadCafeSummaries(candidateIds);

        // 정규화 기준
        double maxReviews = summaries.stream().map(s -> s.reviewCount != null ? s.reviewCount : 0)
                .mapToDouble(Integer::doubleValue).max().orElse(1);

        List<RecommendedCafeDto> out = new ArrayList<>();
        for (var s : summaries) {
            Map<String, Double> cvec = cafeTags.getOrDefault(s.cafeId, Map.of());
            double tagScore = cosine(baseUnit, unit(cvec));

            double ratingScore = s.avgRating != null ? Math.max(0, Math.min(1, s.avgRating / 5.0)) : 0;
            double popularity = Math.min(1.0, (s.reviewCount != null ? s.reviewCount : 0) / Math.max(1.0, maxReviews));

            // 유사 추천은 태그 비중을 더 키움
            double score = 0.70 * tagScore + 0.20 * ratingScore + 0.10 * popularity;

            out.add(RecommendedCafeDto.builder()
                    .cafeId(s.cafeId)
                    .name(s.name)
                    .address(s.address)
                    .mainPhotoUrl(s.mainPhotoUrl)
                    .avgRating(s.avgRating)
                    .reviewCount(s.reviewCount)
                    .tagScore(tagScore)
                    .ratingScore(ratingScore)
                    .popularity(popularity)
                    .recency(0) // 표시용 X
                    .score(score)
                    .build());
        }
        return out.stream()
                .sorted(Comparator.comparingDouble(RecommendedCafeDto::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    // ===== 벡터 유틸 =====
    private Map<String, Double> unit(Map<String, Double> v) {
        if (v == null || v.isEmpty()) return Map.of();
        double norm = Math.sqrt(v.values().stream().mapToDouble(d -> d * d).sum());
        if (norm == 0) return Map.of();
        Map<String, Double> out = new HashMap<>();
        for (var e : v.entrySet()) out.put(e.getKey(), e.getValue() / norm);
        return out;
    }

    private double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        // Iterate smaller
        Map<String, Double> s = a.size() <= b.size() ? a : b;
        Map<String, Double> l = a.size() <= b.size() ? b : a;
        double dot = 0.0;
        for (var e : s.entrySet()) {
            Double bv = l.get(e.getKey());
            if (bv != null) dot += e.getValue() * bv;
        }
        // a,b는 이미 unit 처리된 벡터가 들어오도록 사용
        return Math.max(0.0, Math.min(1.0, dot));
    }
}
