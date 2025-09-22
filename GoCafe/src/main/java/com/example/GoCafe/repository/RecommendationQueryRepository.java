package com.example.GoCafe.repository;

import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 주의: 아래 테이블/컬럼명은 기준 코드의 명명 규칙을 가정.
 * - cafe(id, name, address, main_photo_url ...)
 * - review(id, cafe_id, member_id, rating, created_at ...)
 * - cafe_tag(id, cafe_id, tag_name, weight ...)  // 리뷰태그가 집계되어 채워진 테이블
 * - review_tag(id, review_id, tag_name, weight ...) // (있는 경우만 사용, 안전하게 optional)
 *
 * 이름이 다르면 쿼리의 테이블/컬럼명을 엑셀 매핑표에 맞춰 바꿔 주세요.
 */
@Repository
public class RecommendationQueryRepository {

    @PersistenceContext
    private EntityManager em;

    public static class CafeSummary {
        public Long cafeId;
        public String name;
        public String address;
        public String mainPhotoUrl;
        public Double avgRating;
        public Integer reviewCount;
        public LocalDateTime lastReviewAt;
    }

    /** 카페 요약: 평균 평점 / 리뷰수 / 최근 리뷰일 */
    public List<CafeSummary> loadCafeSummaries(Collection<Long> cafeIds) {
        if (cafeIds == null || cafeIds.isEmpty()) return List.of();

        String sql = """
            SELECT c.id AS cafe_id,
                   c.name,
                   c.address,
                   c.main_photo_url,
                   AVG(r.rating)       AS avg_rating,
                   COUNT(r.id)         AS review_count,
                   MAX(r.created_at)   AS last_review_at
            FROM cafe c
            LEFT JOIN review r ON r.cafe_id = c.id
            WHERE c.id IN :ids
            GROUP BY c.id, c.name, c.address, c.main_photo_url
            """;
        var rows = em.createNativeQuery(sql)
                .setParameter("ids", cafeIds)
                .getResultList();

        List<CafeSummary> list = new ArrayList<>();
        for (Object rowObj : rows) {
            Object[] row = (Object[]) rowObj;
            CafeSummary s = new CafeSummary();
            s.cafeId       = ((Number) row[0]).longValue();
            s.name         = (String) row[1];
            s.address      = (String) row[2];
            s.mainPhotoUrl = (String) row[3];
            s.avgRating    = row[4] != null ? ((Number) row[4]).doubleValue() : null;
            s.reviewCount  = row[5] != null ? ((Number) row[5]).intValue() : 0;
            s.lastReviewAt = row[6] != null ? (row[6] instanceof java.sql.Timestamp
                    ? ((java.sql.Timestamp) row[6]).toLocalDateTime()
                    : (LocalDateTime) row[6]) : null;
            list.add(s);
        }
        return list;
    }

    /** 모든 카페의 태그 벡터 로드: cafe_id -> (tag -> weight) */
    public Map<Long, Map<String, Double>> loadAllCafeTags() {
        String sql = """
            SELECT ct.cafe_id, ct.tag_name, ct.weight
            FROM cafe_tag ct
            """;
        var rows = em.createNativeQuery(sql).getResultList();
        Map<Long, Map<String, Double>> map = new HashMap<>();
        for (Object rowObj : rows) {
            Object[] row = (Object[]) rowObj;
            Long cafeId = ((Number) row[0]).longValue();
            String tag  = (String) row[1];
            Double w    = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            map.computeIfAbsent(cafeId, k -> new HashMap<>())
                    .merge(tag, w, Double::sum);
        }
        return map;
    }

    /**
     * 사용자 선호 태그 벡터: member가 남긴 리뷰의 태그(있으면 review_tag, 없으면 최근 본/좋아요 기반 등으로 대체 가능)
     * 기본 구현은 review_tag 기준, 없으면 cafe_tag를 리뷰한 카페들의 태그 평균으로 보정.
     */
    public Map<String, Double> loadUserPreferenceTags(Long memberId, int recentLimit) {
        // 1) review_tag 기반
        String sqlTags = """
            SELECT rt.tag_name, SUM(rt.weight) AS w
            FROM review r
            JOIN review_tag rt ON rt.review_id = r.id
            WHERE r.member_id = :memberId
            ORDER BY r.created_at DESC
            FETCH FIRST :lim ROWS ONLY
            """;
        @SuppressWarnings("unchecked")
        List<Object[]> tagRows = em.createNativeQuery(sqlTags)
                .setParameter("memberId", memberId)
                .setParameter("lim", recentLimit)
                .getResultList();
        Map<String, Double> pref = new HashMap<>();
        for (Object[] row : tagRows) {
            String tag = (String) row[0];
            Double w   = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            pref.merge(tag, w, Double::sum);
        }
        if (!pref.isEmpty()) return pref;

        // 2) fallback: 사용자가 리뷰한 카페들의 cafe_tag 평균
        String sqlCafeTags = """
            SELECT ct.tag_name, AVG(ct.weight) AS w
            FROM review r
            JOIN cafe_tag ct ON ct.cafe_id = r.cafe_id
            WHERE r.member_id = :memberId
            GROUP BY ct.tag_name
            """;
        @SuppressWarnings("unchecked")
        List<Object[]> rows2 = em.createNativeQuery(sqlCafeTags)
                .setParameter("memberId", memberId)
                .getResultList();
        for (Object[] row : rows2) {
            String tag = (String) row[0];
            Double w   = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            pref.merge(tag, w, Double::sum);
        }
        return pref;
    }

    /** 후보 카페 ID들: 필터가 없으면 전체, 있으면 지역/태그 등으로 제한 가능 */
    public List<Long> loadCandidateCafeIds(int limit) {
        String sql = """
            SELECT c.id
            FROM cafe c
            FETCH FIRST :lim ROWS ONLY
            """;
        @SuppressWarnings("unchecked")
        List<Number> ids = em.createNativeQuery(sql)
                .setParameter("lim", limit)
                .getResultList();
        return ids.stream().map(Number::longValue).collect(Collectors.toList());
    }
}
