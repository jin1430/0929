// src/main/java/com/example/GoCafe/service/TagAggregationService.java
package com.example.GoCafe.service;

import com.example.GoCafe.dto.TagAggregationProperties;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafeTag;
import com.example.GoCafe.repository.CafeTagRepository;
import com.example.GoCafe.repository.ReviewRepository;
import com.example.GoCafe.repository.ReviewTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TagAggregationService {
    private final ReviewRepository reviewRepo;
    private final ReviewTagRepository reviewTagRepo;
    private final CafeTagRepository cafeTagRepo;
    private final TagAggregationProperties props;

    @Transactional
    public void recomputeForCafe(Long cafeId) {
        long total = reviewRepo.countByCafe_Id(cafeId);          // 전체 리뷰 수
        if (total < props.getNMin()) return;                     // 리뷰 부족 시 유지

        var rows = reviewTagRepo.findRowsForCafe(cafeId, props.getTau()); // τ 이상만

        // === 집계: support만 ===
        Map<String, Integer> support = new HashMap<>();          // key = "CAT|code"
        for (var r : rows) {
            String key = r.getCategoryCode() + "|" + r.getCode();
            support.merge(key, 1, Integer::sum);
        }

        // === 후보 필터링: coverage 기반 + 카테고리별 C_min ===
        Map<String, Double> candidateScore = new HashMap<>();    // 최종 score = coverage(소수 4자리)
        for (var e : support.entrySet()) {
            String key = e.getKey();                              // 예: "VIBE|모던/미니멀"
            int sc = e.getValue();
            double cov = total == 0 ? 0.0 : (double) sc / (double) total;

            String category = key.substring(0, key.indexOf('|'));
            String code = key.substring(key.indexOf('|') + 1);

            // 카테고리별 coverage 하한
            double cMinEff = props.getEffectiveCmin(category);

            // PRICE: support 하한 강화(옵션)
            int sMinEff = props.getSMin();
            if ("PRICE".equals(category)) {
                int sMinByRatio = (int) Math.ceil(props.getSMinPriceRatio() * total);
                sMinEff = Math.max(sMinEff, sMinByRatio);
            }

            // === VIBE 전용 추가 하한 ===
            if ("VIBE".equals(category)) {
                // 기본 하한: support/coverage
                if (!(sc >= Math.max(sMinEff, props.getVibeSupportMin()) && cov >= cMinEff)) {
                    continue; // 후보 탈락
                }
                // ✅ VIBE 전체(자연 포함) 기준 점유율 계산: 40% 미만이면 보류
                int vibeTotal = support.entrySet().stream()
                        .filter(x -> x.getKey().startsWith("VIBE|"))
                        .mapToInt(Map.Entry::getValue).sum();
                double share = vibeTotal == 0 ? 0.0 : (double) sc / (double) vibeTotal;
                if (share < props.getVibeShareMin()) continue;
            } else {
                // 비-VIBE 공통
                if (!(sc >= sMinEff && cov >= cMinEff)) continue;
            }

            // 최종 점수: coverage (절대 가중치 γ는 현재 미적용; 필요시 +γ 추가)
            candidateScore.put(key, round4(cov));
        }

        // === 상충 처리 ===
        resolveVibeConflicts(candidateScore, support, props.getDelta());       // nature 공존, 나머지 배타(근소차 보류)
        resolvePriceConflict(candidateScore, support, props.getDeltaPrice());  // PRICE 단일선택(근소차 시 support 우선)

        // === 정렬 후 Top-K 업서트 ===
        var sorted = candidateScore.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(props.getTopK())
                .toList();

        // 기존 태그 싹 지우고 새로 저장
        cafeTagRepo.deleteByCafe_Id(cafeId);

        int rank = 1;
        for (var ent : sorted) {
            String[] parts = ent.getKey().split("\\|", 2);
            cafeTagRepo.save(new CafeTag(
                    null, new Cafe(){ { setId(cafeId);} },
                    parts[0], parts[1], rank++, ent.getValue()
            ));
        }
    }

    // ===== helpers =====

    // VIBE: '자연'만 공존 허용, 나머지는 전부 배타군 취급
    private static boolean isNature(String code) {
        return "자연".equals(code);
    }

    /** VIBE: nature 공존, 배타군은 최고 1개만. 근소차(<delta)면 둘 다 보류 */
    private void resolveVibeConflicts(Map<String, Double> cs,
                                      Map<String, Integer> support,
                                      double delta) {
        var vibeKeys = cs.keySet().stream()
                .filter(k -> k.startsWith("VIBE|"))
                .toList();
        if (vibeKeys.isEmpty()) return;

        // 배타군만 추림 (자연 제외)
        var exclusive = vibeKeys.stream()
                .filter(k -> !k.endsWith("|자연"))
                .toList();
        if (exclusive.isEmpty()) return;

        var top = exclusive.stream().max(Comparator.comparingDouble(cs::get)).orElse(null);
        var second = exclusive.stream().filter(k -> !k.equals(top))
                .max(Comparator.comparingDouble(cs::get)).orElse(null);

        if (top == null || second == null) return;

        if (Math.abs(cs.get(top) - cs.get(second)) < delta) {
            // 근소차 → 모두 보류
            exclusive.forEach(cs::remove);
            return;
        }
        // top 제외 제거
        exclusive.stream().filter(k -> !k.equals(top)).forEach(cs::remove);
    }

    /** PRICE: 내부에서 단일선택. 근소차(<deltaPrice)면 support 큰 쪽, 동률이면 '중간' 우선 */
    private void resolvePriceConflict(Map<String, Double> cs,
                                      Map<String, Integer> support,
                                      double deltaPrice) {
        var priceKeys = cs.keySet().stream()
                .filter(k -> k.startsWith("PRICE|"))
                .toList();
        if (priceKeys.size() <= 1) return;

        var top = priceKeys.stream().max(Comparator.comparingDouble(cs::get)).orElse(null);
        var second = priceKeys.stream().filter(k -> !k.equals(top))
                .max(Comparator.comparingDouble(cs::get)).orElse(null);
        if (top == null || second == null) return;

        if (Math.abs(cs.get(top) - cs.get(second)) < deltaPrice) {
            int s1 = support.getOrDefault(top, 0);
            int s2 = support.getOrDefault(second, 0);
            String pick;
            if (s1 != s2) {
                pick = (s1 > s2 ? top : second);
            } else {
                // 동률이면 '중간' 우선
                pick = priceKeys.stream().filter(k -> k.endsWith("|중간"))
                        .findFirst().orElse(top);
            }
            for (var k : priceKeys) if (!k.equals(pick)) cs.remove(k);
        } else {
            priceKeys.stream().filter(k -> !k.equals(top)).forEach(cs::remove);
        }
    }

    private static double round4(double v) { return Math.round(v * 1e4) / 1e4; }

    // (지금은 전체 삭제 후 insert로 가서 안 쓰지만, 참고용/복구용으로 남겨둠)
    private void upsertCafeTag(Long cafeId, String category, String code, int rankNo, double score) {
        var existing = cafeTagRepo.findByCafe_IdAndTagCode(cafeId, code).orElse(null);
        if (existing == null) {
            var cafe = new Cafe(); cafe.setId(cafeId);
            cafeTagRepo.save(new CafeTag(null, cafe, category, code, rankNo, score));
        } else {
            existing.setCategoryCode(category);
            existing.setRankNo(rankNo);
            existing.setScore(score);
            cafeTagRepo.save(existing);
        }
    }
}
