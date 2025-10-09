// src/main/java/com/example/GoCafe/service/RecommendService.java
package com.example.GoCafe.service;

import com.example.GoCafe.dto.CafeRecommendDto;
import com.example.GoCafe.dto.QuestionsForm;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafePhoto;
import com.example.GoCafe.entity.CafeTag;
import com.example.GoCafe.repository.CafePhotoRepository;
import com.example.GoCafe.repository.CafeTagRepository;
import com.example.GoCafe.repository.UserNeedsRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {

    private final CafePhotoRepository cafePhotoRepository;
    private final CafeTagRepository cafeTagRepository;
    private final UserNeedsRepository userNeedsRepository;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean loaded = new AtomicBoolean(false);
    private Map<String, OnboardingMapping> index = Map.of(); // key: purpose|vibe|factor|time|area
    private List<OnboardingMapping> mappings = List.of();

    private int resolveAgeBucketIndex(Long age) {
        if (age == null || age < 10) return -1;
        if (age < 20) return 0;
        if (age < 30) return 1;
        if (age < 40) return 2;
        if (age < 50) return 3;
        return 4;
    }

    /* ====== 공통 정렬(동점 그룹 셔플) ====== */
    private List<CafeRecommendDto> sortDescThenShuffleTies(List<CafeRecommendDto> in) {
        if (in == null || in.isEmpty()) return in;
        final int SCALE = 4;
        Map<java.math.BigDecimal, List<CafeRecommendDto>> buckets = new HashMap<>();
        for (CafeRecommendDto dto : in) {
            double s = dto.getScore();
            if (Double.isNaN(s) || Double.isInfinite(s)) s = 0.0;
            java.math.BigDecimal key = java.math.BigDecimal
                    .valueOf(s)
                    .setScale(SCALE, java.math.RoundingMode.HALF_UP);
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(dto);
        }
        List<java.math.BigDecimal> keys = new ArrayList<>(buckets.keySet());
        keys.sort(Comparator.reverseOrder());
        java.util.Random rnd = java.util.concurrent.ThreadLocalRandom.current();
        List<CafeRecommendDto> out = new ArrayList<>(in.size());
        for (java.math.BigDecimal k : keys) {
            List<CafeRecommendDto> group = buckets.get(k);
            java.util.Collections.shuffle(group, rnd);
            out.addAll(group);
        }
        return out;
    }

    /* ====== 성별 추천: UserNeeds.weight × CSV(M/F) ====== */
    @Transactional(readOnly = true)
    public List<CafeRecommendDto> recommendByGender(String gender, Long memberId, List<Cafe> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();

        // 후보 카페 태그
        Set<Long> cafeIds = candidates.stream().map(Cafe::getId).collect(Collectors.toSet());
        List<CafeTag> allTags = cafeTagRepository.findByCafeIdIn(cafeIds);

        // CSV 가중치
        Map<String, double[]> weightMap = loadGenderWeightsFromCsv(); // "category||tag" → [M,F]
        String g = gender == null ? "" : gender.trim().toUpperCase(Locale.ROOT);

        // UserNeeds 가중치
        Map<String, Double> needs = loadUserNeedsWeights(memberId);   // "category||tag" → weight

        Map<Long, List<CafeTagScore>> perCafeScores = new HashMap<>();
        for (CafeTag ct : allTags) {
            String category = nullToEmpty(ct.getCategoryCode());
            String tag      = nullToEmpty(ct.getTagCode());
            String key      = category.toLowerCase(Locale.ROOT) + "||" + tag.toLowerCase(Locale.ROOT);

            double[] mf = weightMap.getOrDefault(key, new double[]{1.0, 1.0});
            double wCsv = switch (g) { case "M" -> mf[0]; case "F" -> mf[1]; default -> 1.0; };

            // 핵심: UserNeeds.weight (없으면 1.0)
            double base = needs.getOrDefault(key, 1.0);
            double finalScore = base * wCsv;

            perCafeScores
                    .computeIfAbsent(ct.getCafe().getId(), k -> new ArrayList<>())
                    .add(new CafeTagScore(category, tag, finalScore));
        }

        Map<Long, String> photoMap = findMainPhotoUrlMap(cafeIds);

        Map<Long, Cafe> cafeById = candidates.stream().collect(Collectors.toMap(Cafe::getId, c -> c));
        List<CafeRecommendDto> out = new ArrayList<>(candidates.size());
        for (Long id : cafeIds) {
            Cafe c = cafeById.get(id);
            List<CafeTagScore> scores = perCafeScores.getOrDefault(id, List.of());
            double total = scores.stream().mapToDouble(s -> s.finalScore).sum();
            List<String> topTags = scores.stream()
                    .sorted(Comparator.comparingDouble((CafeTagScore s) -> s.finalScore).reversed())
                    .limit(3)
                    .map(s -> s.categoryCode + ":" + s.tagCode)
                    .toList();

            out.add(CafeRecommendDto.builder()
                    .cafeId(id)
                    .name(c.getName())
                    .address(c.getAddress())
                    .views(c.getViews() == null ? null : c.getViews().longValue())
                    .photoUrl(photoMap.getOrDefault(id, ""))
                    .score(total)
                    .topTags(topTags)
                    .build());
        }

        out.sort(Comparator.comparingDouble(CafeRecommendDto::getScore).reversed());
        return sortDescThenShuffleTies(out);
    }

    /* ====== 연령 추천: UserNeeds.weight × CSV(10/20/30/40/50) ====== */
    @Transactional(readOnly = true)
    public List<CafeRecommendDto> recommendByAge(Long age, Long memberId, List<Cafe> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();

        Set<Long> cafeIds = candidates.stream().map(Cafe::getId).collect(Collectors.toSet());
        List<CafeTag> allTags = cafeTagRepository.findByCafeIdIn(cafeIds);

        Map<String, double[]> weightMap = loadAgeWeightsFromCsv(); // "category||tag" → [10s..50s]
        int bucket = resolveAgeBucketIndex(age);

        Map<String, Double> needs = loadUserNeedsWeights(memberId);   // "category||tag" → weight

        Map<Long, List<CafeTagScore>> perCafeScores = new HashMap<>();
        for (CafeTag ct : allTags) {
            String category = nullToEmpty(ct.getCategoryCode());
            String tag      = nullToEmpty(ct.getTagCode());
            String key      = category.toLowerCase(Locale.ROOT) + "||" + tag.toLowerCase(Locale.ROOT);

            double[] arr = weightMap.getOrDefault(key, new double[]{1,1,1,1,1});
            double wCsv  = (bucket >= 0 && bucket < arr.length) ? arr[bucket] : 1.0;

            double base = needs.getOrDefault(key, 1.0);
            double finalScore = base * wCsv;

            perCafeScores
                    .computeIfAbsent(ct.getCafe().getId(), k -> new ArrayList<>())
                    .add(new CafeTagScore(category, tag, finalScore));
        }

        Map<Long, String> photoMap = findMainPhotoUrlMap(cafeIds);

        Map<Long, Cafe> cafeById = candidates.stream().collect(Collectors.toMap(Cafe::getId, c -> c));
        List<CafeRecommendDto> out = new ArrayList<>(candidates.size());
        for (Long id : cafeIds) {
            Cafe c = cafeById.get(id);
            List<CafeTagScore> scores = perCafeScores.getOrDefault(id, List.of());
            double total = scores.stream().mapToDouble(s -> s.finalScore).sum();

            List<String> topTags = scores.stream()
                    .sorted(Comparator.comparingDouble((CafeTagScore s) -> s.finalScore).reversed())
                    .limit(3)
                    .map(s -> s.categoryCode + ":" + s.tagCode)
                    .toList();

            out.add(CafeRecommendDto.builder()
                    .cafeId(id)
                    .name(c.getName())
                    .address(c.getAddress())
                    .views(c.getViews() == null ? null : c.getViews().longValue())
                    .photoUrl(photoMap.getOrDefault(id, ""))
                    .score(total)
                    .topTags(topTags)
                    .build());
        }

        out.sort(
                Comparator.comparingDouble(CafeRecommendDto::getScore).reversed()
                        .thenComparing(Comparator.comparing(CafeRecommendDto::getViews,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .thenComparing(CafeRecommendDto::getCafeId, Comparator.nullsLast(Comparator.reverseOrder()))
        );

        return sortDescThenShuffleTies(out);
    }

    /* ====== 질문 기반(기존 로직) ====== */
    @Transactional(readOnly = true)
    public List<CafeRecommendDto> recommendByQuestions(QuestionsForm form, List<Cafe> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();

        ProfileResult pr = resolveProfileByQuestions(form);
        Map<String, Double> weightsRaw = (pr == null || pr.weights == null) ? Map.of() : pr.weights;

        Set<Long> cafeIds = candidates.stream().map(Cafe::getId).collect(Collectors.toSet());
        List<CafeTag> allTags = cafeTagRepository.findByCafeIdIn(cafeIds);

        Map<String, Double> weightsLookup = buildQuestionsWeightLookup(weightsRaw);

        record CafeTagScore(String categoryCode, String tagCode, double finalScore) {}

        Map<Long, List<CafeTagScore>> perCafeScores = new HashMap<>();
        for (CafeTag ct : allTags) {
            String category = nullToEmpty(ct.getCategoryCode());
            String tag      = nullToEmpty(ct.getTagCode());

            String keyDot  = category.toUpperCase(Locale.ROOT) + "." + tag.toLowerCase(Locale.ROOT);
            String keyPipe = category.toLowerCase(Locale.ROOT) + "||" + tag.toLowerCase(Locale.ROOT);

            double w = weightsLookup.getOrDefault(keyDot,
                    weightsLookup.getOrDefault(keyPipe, 1.0));

            double base = (ct.getScore() == null ? 0.0 : ct.getScore());
            double finalScore = base * w;

            perCafeScores
                    .computeIfAbsent(ct.getCafe().getId(), k -> new ArrayList<>())
                    .add(new CafeTagScore(category, tag, finalScore));
        }

        Map<Long, String> photoMap = findMainPhotoUrlMap(cafeIds);

        Map<Long, Cafe> cafeById = candidates.stream().collect(Collectors.toMap(Cafe::getId, c -> c));
        List<CafeRecommendDto> out = new ArrayList<>(candidates.size());

        for (Long id : cafeIds) {
            Cafe c = cafeById.get(id);
            List<CafeTagScore> scores = perCafeScores.getOrDefault(id, List.of());

            double total = scores.stream().mapToDouble(s -> s.finalScore).sum();

            List<String> topTags = scores.stream()
                    .sorted(Comparator.comparingDouble((CafeTagScore s) -> s.finalScore).reversed())
                    .limit(3)
                    .map(s -> s.categoryCode + ":" + s.tagCode)
                    .toList();

            out.add(CafeRecommendDto.builder()
                    .cafeId(id)
                    .name(c.getName())
                    .address(c.getAddress())
                    .views(c.getViews() == null ? null : c.getViews().longValue())
                    .photoUrl(photoMap.getOrDefault(id, ""))
                    .score(total)
                    .topTags(topTags)
                    .build());
        }

        out.sort(
                Comparator.comparingDouble(CafeRecommendDto::getScore).reversed()
                        .thenComparing(Comparator.comparing(CafeRecommendDto::getViews,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .thenComparing(CafeRecommendDto::getCafeId, Comparator.nullsLast(Comparator.reverseOrder()))
        );

        return sortDescThenShuffleTies(out);
    }

    /* ====== 기본 추천(변경 없음) ====== */
    // RecommendService.java (메서드 전체 교체)
    @Transactional(readOnly = true)
    public List<CafeRecommendDto> recommendByBaseNeeds(Long memberId, List<Cafe> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();

        // 후보 카페 태그 전부
        Set<Long> cafeIds = candidates.stream().map(Cafe::getId).collect(Collectors.toSet());
        List<CafeTag> allTags = cafeTagRepository.findByCafeIdIn(cafeIds);

        // ★ 핵심: 사용자 취향(UserNeeds)만 사용 — "category||tag" → needsWeight
        Map<String, Double> needs = loadUserNeedsWeights(memberId); // 비로그인/미설정이면 빈 맵

        // 카페별 점수(= UserNeeds weight 합)와 태그 기여도 수집
        record NeedScore(String categoryCode, String tagCode, double w) {}
        Map<Long, List<NeedScore>> perCafe = new HashMap<>();

        for (CafeTag ct : allTags) {
            String category = nullToEmpty(ct.getCategoryCode());
            String tag      = nullToEmpty(ct.getTagCode());
            String key      = category.toLowerCase(Locale.ROOT) + "||" + tag.toLowerCase(Locale.ROOT);

            double w = needs.getOrDefault(key, 0.0);   // ★ UserNeeds에 없으면 0점(기여 없음)
            if (w <= 0) continue;                      // 0 이하는 스킵

            perCafe.computeIfAbsent(ct.getCafe().getId(), k -> new ArrayList<>())
                    .add(new NeedScore(category, tag, w));
        }

        // 대표 사진
        Map<Long, String> photoMap = findMainPhotoUrlMap(cafeIds);

        // DTO 구성
        Map<Long, Cafe> cafeById = candidates.stream().collect(Collectors.toMap(Cafe::getId, c -> c));
        List<CafeRecommendDto> out = new ArrayList<>(candidates.size());

        for (Long id : cafeIds) {
            Cafe c = cafeById.get(id);
            List<NeedScore> list = perCafe.getOrDefault(id, List.of());

            double total = list.stream().mapToDouble(ns -> ns.w).sum(); // ★ 합산 = UserNeeds weight의 합

            // 상위 태그는 기여도(w) 기준
            List<String> top3 = list.stream()
                    .sorted(Comparator.comparingDouble((NeedScore ns) -> ns.w).reversed())
                    .limit(3)
                    .map(ns -> ns.categoryCode + ":" + ns.tagCode)
                    .toList();

            out.add(CafeRecommendDto.builder()
                    .cafeId(id)
                    .name(c.getName())
                    .address(c.getAddress())
                    .views(c.getViews() == null ? null : c.getViews().longValue())
                    .photoUrl(photoMap.getOrDefault(id, ""))
                    .score(total)              // ★ 총점 = UserNeeds 가중치 합 (예: 모던 0.9 + 콜드브루 1.2 = 2.1)
                    .topTags(top3)
                    .build());
        }

        // 총점 내림차순 + 보조 타이브레이커 (뷰수, id) + 동점 랜덤 섞기
        out.sort(
                Comparator.comparingDouble(CafeRecommendDto::getScore).reversed()
                        .thenComparing(Comparator.comparing(CafeRecommendDto::getViews,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .thenComparing(CafeRecommendDto::getCafeId, Comparator.nullsLast(Comparator.reverseOrder()))
        );
        return sortDescThenShuffleTies(out);
    }


    /* ====== UserNeeds 로드 ====== */
    private Map<String, Double> loadUserNeedsWeights(Long memberId) {
        if (memberId == null) return Map.of();
        try {
            return userNeedsRepository.findByMemberId(memberId).stream()
                    .collect(Collectors.toMap(
                            u -> nullToEmpty(u.getCategoryCode()).toLowerCase(Locale.ROOT)
                                    + "||"
                                    + nullToEmpty(u.getCode()).toLowerCase(Locale.ROOT),
                            u -> (u.getWeight() == null ? 1.0 : u.getWeight()),
                            (a, b) -> b,
                            LinkedHashMap::new
                    ));
        } catch (Exception e) {
            log.warn("[Recommend] failed to load UserNeeds for memberId={}", memberId, e);
            return Map.of();
        }
    }

    /* ====== CSV 로더들 ====== */
    private Map<String, double[]> loadGenderWeightsFromCsv() {
        Map<String, double[]> map = new HashMap<>();
        try {
            ClassPathResource res = new ClassPathResource("data/cafe_tag_gender_weights.csv");
            if (!res.exists()) {
                log.warn("[Recommend] CSV not found: classpath:data/cafe_tag_gender_weights.csv (defaults=1.0)");
                return map;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
                String header = br.readLine();
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] cols = line.split(",", -1);
                    if (cols.length < 4) continue;

                    String category = cols[0].trim().toLowerCase(Locale.ROOT);
                    String tag      = cols[1].trim().toLowerCase(Locale.ROOT);
                    double m        = parseOrDefault(cols[2], 1.0);
                    double f        = parseOrDefault(cols[3], 1.0);

                    map.put(category + "||" + tag, new double[]{m, f});
                }
            }
        } catch (Exception e) {
            log.error("[Recommend] failed to load gender weights CSV", e);
        }
        return map;
    }

    private Map<String, double[]> loadAgeWeightsFromCsv() {
        Map<String, double[]> map = new HashMap<>();
        try {
            ClassPathResource res = new ClassPathResource("data/cafe_tag_age_weights.csv");
            if (!res.exists()) {
                log.warn("[Recommend] CSV not found: classpath:data/cafe_tag_age_weights.csv (defaults=1.0)");
                return map;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
                String header = br.readLine();
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] cols = line.split(",", -1);
                    if (cols.length < 7) continue;

                    String category = cols[0].trim().toLowerCase(Locale.ROOT);
                    String tag      = cols[1].trim().toLowerCase(Locale.ROOT);
                    double w10 = parseOrDefault(cols[2], 1.0);
                    double w20 = parseOrDefault(cols[3], 1.0);
                    double w30 = parseOrDefault(cols[4], 1.0);
                    double w40 = parseOrDefault(cols[5], 1.0);
                    double w50 = parseOrDefault(cols[6], 1.0);

                    map.put(category + "||" + tag, new double[]{w10, w20, w30, w40, w50});
                }
            }
            log.info("[Recommend] loaded age weights rows={}", map.size());
        } catch (Exception e) {
            log.error("[Recommend] CSV load failed (age weights), using defaults", e);
        }
        return map;
    }

    /* ====== 질문용 프로필/매핑 로딩 ====== */
    public ProfileResult resolveProfileByQuestions(QuestionsForm form) {
        ensureLoaded();

        String exactKey = buildKey(form.getPurpose(), form.getVibe(), form.getFactor(), form.getTime(), form.getArea());
        OnboardingMapping exact = index.get(exactKey);
        if (exact != null) {
            return new ProfileResult(exact.title, exact.description, exact.weights);
        }

        final int MIN_MATCH = 3;

        List<OnboardingMapping> candidates = new ArrayList<>();
        for (OnboardingMapping m : mappings) {
            if (matchCount(form, m) >= MIN_MATCH) {
                candidates.add(m);
            }
        }

        if (candidates.isEmpty()) {
            Map<String, Double> fallback = new LinkedHashMap<>();
            if (form.getPurpose() != null) fallback.put("PURPOSE." + form.getPurpose(), 1.10);
            if (form.getVibe()    != null) fallback.put("VIBE."    + form.getVibe(),    1.10);
            return new ProfileResult(
                    "기본 추천",
                    "입력 조합에 해당하는 프로필이 없어 기본 보정으로 추천해요.",
                    fallback
            );
        }

        candidates.sort((a, b) -> {
            int cmp = Double.compare(score(form, b), score(form, a));
            if (cmp != 0) return cmp;
            return nullToEmpty(a.title).compareToIgnoreCase(nullToEmpty(b.title));
        });

        int bestMatchCount = matchCount(form, candidates.get(0));
        if (bestMatchCount >= 4) {
            OnboardingMapping m = candidates.get(0);
            return new ProfileResult(m.title, m.description, m.weights);
        }

        List<OnboardingMapping> top = candidates.subList(0, Math.min(3, candidates.size()));
        Map<String, Double> merged = new LinkedHashMap<>();
        double weightSum = 0.0;

        for (OnboardingMapping m : top) {
            int w = matchCount(form, m);
            double sw = (double) w;
            weightSum += sw;
            if (m.weights != null) {
                for (Map.Entry<String, Double> e : m.weights.entrySet()) {
                    String k = e.getKey();
                    double v = (e.getValue() == null ? 1.0 : e.getValue());
                    merged.merge(k, v * sw, Double::sum);
                }
            }
        }

        if (weightSum > 0) {
            for (Map.Entry<String, Double> e : merged.entrySet()) {
                merged.put(e.getKey(), e.getValue() / weightSum);
            }
        }

        if (form.getPurpose() != null) {
            merged.merge("PURPOSE." + form.getPurpose(), 1.05, Math::max);
        }
        if (form.getVibe() != null) {
            merged.merge("VIBE." + form.getVibe(), 1.05, Math::max);
        }

        String title = "혼합 프로필 추천";
        String desc  = top.stream().map(m -> nullToEmpty(m.title))
                .filter(s -> !s.isBlank())
                .limit(3)
                .collect(Collectors.joining(" + "));

        if (desc.isBlank()) desc = "가장 유사한 여러 프로필을 합성했어요.";

        return new ProfileResult(title, desc, merged);
    }

    private void ensureLoaded() {
        if (loaded.get()) return;
        synchronized (this) {
            if (loaded.get()) return;
            try {
                ClassPathResource res = new ClassPathResource("templates/question_result_mapping.json");
                try (InputStream is = res.getInputStream()) {
                    List<OnboardingMapping> list = objectMapper.readValue(
                            is, new TypeReference<List<OnboardingMapping>>() {}
                    );
                    Map<String, OnboardingMapping> tmp = new HashMap<>();
                    for (OnboardingMapping m : list) {
                        String k = buildKey(m.purpose, m.vibe, m.factor, m.time, m.area);
                        tmp.put(k, m);
                    }
                    this.index = Collections.unmodifiableMap(tmp);
                    this.mappings = Collections.unmodifiableList(list);
                    loaded.set(true);
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load question_result_mapping.json", e);
            }
        }
    }

    /* ====== 보조 유틸 ====== */
    private Map<Long, String> findMainPhotoUrlMap(Set<Long> cafeIds) {
        Map<Long, String> photoMap = Collections.emptyMap();
        try {
            List<CafePhoto> mainPhotos = cafePhotoRepository.findForCafeIdsOrderByMainThenSort(cafeIds);
            photoMap = mainPhotos.stream().collect(Collectors.toMap(
                    p -> p.getCafe().getId(),
                    CafePhoto::getUrl,
                    (a, b) -> a
            ));
        } catch (Exception ignore) {}
        return photoMap;
    }

    private static double parseOrDefault(String s, double d) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return d; }
    }
    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static boolean eq(String a, String b) {
        return Objects.equals(nullToEmpty(a).toLowerCase(Locale.ROOT),
                nullToEmpty(b).toLowerCase(Locale.ROOT));
    }
    private static boolean eqWithAnywhere(String a, String b) {
        String x = nullToEmpty(a).toLowerCase(Locale.ROOT);
        String y = nullToEmpty(b).toLowerCase(Locale.ROOT);
        if (x.isEmpty() || y.isEmpty()) return false;
        if ("anywhere".equals(x) || "anywhere".equals(y)) return true;
        return x.equals(y);
    }
    private static int matchCount(QuestionsForm q, OnboardingMapping m) {
        int c = 0;
        if (!nullToEmpty(q.getPurpose()).isEmpty() && eq(q.getPurpose(), m.purpose)) c++;
        if (!nullToEmpty(q.getVibe()).isEmpty()    && eq(q.getVibe(),    m.vibe))    c++;
        if (!nullToEmpty(q.getFactor()).isEmpty()  && eq(q.getFactor(),  m.factor))  c++;
        if (!nullToEmpty(q.getTime()).isEmpty()    && eq(q.getTime(),    m.time))    c++;
        if (!nullToEmpty(q.getArea()).isEmpty()    && eqWithAnywhere(q.getArea(),    m.area)) c++;
        return c;
    }
    private static double score(QuestionsForm q, OnboardingMapping m) {
        int mc = matchCount(q, m);
        double bonus = 0.0;
        if (!nullToEmpty(q.getPurpose()).isEmpty() && eq(q.getPurpose(), m.purpose)) bonus += 0.02;
        if (!nullToEmpty(q.getFactor()).isEmpty()  && eq(q.getFactor(),  m.factor))  bonus += 0.01;
        return mc + bonus;
    }
    private String buildKey(String purpose, String vibe, String factor, String time, String area) {
        return String.join("|",
                nullToEmpty(purpose),
                nullToEmpty(vibe),
                nullToEmpty(factor),
                nullToEmpty(time),
                nullToEmpty(area)
        );
    }
    private Map<String, Double> buildQuestionsWeightLookup(Map<String, Double> raw) {
        if (raw == null || raw.isEmpty()) return Map.of();
        Map<String, Double> out = new HashMap<>(raw.size() * 2);
        for (Map.Entry<String, Double> e : raw.entrySet()) {
            String k = nullToEmpty(e.getKey()).trim();
            Double v = (e.getValue() == null ? 1.0 : e.getValue());
            out.put(k, v);
            int idx = k.indexOf('.');
            if (idx > 0 && idx + 1 < k.length()) {
                String category = k.substring(0, idx).trim();
                String tag = k.substring(idx + 1).trim();
                String pipeKey = category.toLowerCase(Locale.ROOT) + "||" + tag.toLowerCase(Locale.ROOT);
                out.putIfAbsent(pipeKey, v);
            }
        }
        return out;
    }

    /* ====== 내부 DTO/클래스 ====== */
    private static class OnboardingMapping {
        public String purpose;
        public String vibe;
        public String factor;
        public String time;
        public String area;
        public String title;
        public String description;
        public Map<String, Double> weights;
    }
    public static class ProfileResult {
        public final String title;
        public final String description;
        public final Map<String, Double> weights;
        public ProfileResult(String title, String description, Map<String, Double> weights) {
            this.title = title;
            this.description = description;
            this.weights = (weights == null) ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(weights));
        }
    }

    private record CafeTagScore(String categoryCode, String tagCode, double finalScore) {}
}
