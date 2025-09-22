package com.example.GoCafe.controller;

import com.example.GoCafe.dto.CafeCardForm;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafeInfo;
import com.example.GoCafe.entity.CafePhoto;
import com.example.GoCafe.entity.CafeTag;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.Review;
import com.example.GoCafe.repository.CafeInfoRepository;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.service.CafePhotoService;
import com.example.GoCafe.service.CafeService;
import com.example.GoCafe.service.CafeTagService;
import com.example.GoCafe.service.ProGateService;
import com.example.GoCafe.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final CafeService cafeService;
    private final CafeTagService cafeTagService;
    private final ReviewService reviewService;
    private final CafePhotoService cafePhotoService;

    // ✅ 미션/컨텍스트
    private final CafeInfoRepository cafeInfoRepository;
    private final MemberRepository memberRepository;
    private final ProGateService proGateService;

    // ====== 라우트 ======

    @GetMapping({"/main"})
    public String main(Model model, Authentication auth) {
        // 1) 인기 카페(Top 8 by views) → cafeCards
        List<Cafe> topCafes = cafeService.findApprovedTopByViews(8);
        List<CafeCardForm> cafeCards = buildCafeCards(topCafes);
        model.addAttribute("cafeCards", cafeCards);

        // (과도기별칭) 과거 hotCafes를 보던 템플릿 대비
        model.addAttribute("hotCafes", cafeCards);

        // 2) 태그(최대 24개)
        List<CafeTag> cafeTags = cafeTagService.findAll().stream()
                .filter(Objects::nonNull)
                .limit(24)
                .collect(Collectors.toList());
        model.addAttribute("cafeTags", cafeTags);

        // 3) 최신 리뷰(최신 6개) → latestReviews (뷰용 경량 DTO)
        List<Review> recentReviews = reviewService.findAll().stream()
                .sorted(Comparator.comparing(Review::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(6)
                .collect(Collectors.toList());
        model.addAttribute("latestReviews", toLatestReviewCards(recentReviews));

        // (과도기별칭) 기존 recentReviews를 직접 보던 페이지 대비
        model.addAttribute("recentReviews", recentReviews);

        // 4) 미션/로그인 컨텍스트
        addMissionContext(model, auth);

        // 5) 문구(없어도 렌더 OK, 있으면 대체)
        model.addAttribute("homeSubtitle", "전국 10,000+ 카페와 생생한 리뷰를 만나보세요");
        model.addAttribute("hotSubtitle", "많은 사람들이 사랑하는 카페들을 만나보세요");
        model.addAttribute("missionSubtitle", "미션을 완료하고 다양한 혜택을 받아보세요");
        model.addAttribute("cafeSubtitle", "당신에게 완벽한 카페를 찾아보세요");
        model.addAttribute("isHome",true);
        return "page/main";
    }

    @GetMapping("/search")
    public String search(@RequestParam(value = "q", required = false) String q,
                         @RequestParam(value = "tag", required = false) String tag,
                         @RequestParam(value = "category", required = false) String category,
                         Model model,
                         Authentication auth) {

        // 1) 검색 결과 카페 → cafeCards 로 통일
        List<Cafe> results = cafeService.searchApproved(q);
        List<CafeCardForm> cafeCards = buildCafeCards(results);
        model.addAttribute("cafeCards", cafeCards);

        // (과도기별칭) 검색 페이지에서 쓰던 이름들도 잠시 유지
        model.addAttribute("trendingCafes", results); // 필요 없으면 제거 가능
        model.addAttribute("hotCafes", cafeCards);

        // 2) 태그(최대 24개)
        List<CafeTag> tags = cafeTagService.findAll().stream()
                .filter(Objects::nonNull)
                .limit(24)
                .collect(Collectors.toList());
        model.addAttribute("cafeTags", tags);

        // 3) 최신 리뷰(최신 6개) → latestReviews
        List<Review> recent = reviewService.findAll().stream()
                .sorted(Comparator.comparing(Review::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(6)
                .collect(Collectors.toList());
        model.addAttribute("latestReviews", toLatestReviewCards(recent));

        // (과도기별칭)
        model.addAttribute("recentReviews", recent);

        // 4) 검색 컨텍스트
        model.addAttribute("query", q);
        model.addAttribute("selectedTag", tag);
        model.addAttribute("selectedCategory", category);

        // 5) 미션/로그인 컨텍스트
        addMissionContext(model, auth);

        // 6) 문구
        model.addAttribute("homeSubtitle", "전국 10,000+ 카페와 생생한 리뷰를 만나보세요");
        model.addAttribute("hotSubtitle", "많은 사람들이 사랑하는 카페들을 만나보세요");
        model.addAttribute("missionSubtitle", "미션을 완료하고 다양한 혜택을 받아보세요");
        model.addAttribute("cafeSubtitle", "검색/필터로 원하는 카페를 찾아보세요");
        model.addAttribute("isCafes", true);
        return "page/main";
    }

    // ====== 내부 헬퍼 ======

    /** Cafe -> CafeCardForm 리스트 (메인 사진 우선순위 매핑) */
    private List<CafeCardForm> buildCafeCards(List<Cafe> cafes) {
        if (cafes == null || cafes.isEmpty()) return Collections.emptyList();

        Set<Long> ids = cafes.stream().map(Cafe::getId).collect(Collectors.toSet());
        List<CafePhoto> mainPhotos = cafePhotoService.findForCafeIdsOrderByMainThenSort(ids);

        Map<Long, String> photoByCafeId = new HashMap<>();
        for (CafePhoto p : mainPhotos) {
            Long cafeId = p.getCafe().getId();
            // 메인 > 정렬순 첫 번째만 사용
            photoByCafeId.putIfAbsent(cafeId, p.getUrl());
        }

        final String PLACEHOLDER = "/images/placeholder-cafe.jpg";

        return cafes.stream()
                .map(c -> {
                    String url = photoByCafeId.get(c.getId());
                    String safeUrl = (url == null || url.isBlank()) ? PLACEHOLDER : url;
                    return new CafeCardForm(
                            c.getId(),
                            c.getName(),
                            c.getAddress(),
                            c.getPhoneNumber(),
                            c.getBusinessCode(),
                            c.getViews(),
                            safeUrl
                    );
                })
                .collect(Collectors.toList());
    }

    /** Review 엔티티 리스트 → 뷰 요구 스펙에 맞춘 Map 리스트(latestReviews) */
    private List<Map<String, Object>> toLatestReviewCards(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) return Collections.emptyList();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        return reviews.stream().map(rv -> {
            Map<String, Object> m = new HashMap<>();

            String nick = (rv.getMember() != null) ? rv.getMember().getNickname() : "알 수 없음";
            m.put("initial", initialsOf(nick));
            m.put("author", nick);

            // ⭐ 별점: Review에 평점 필드가 없다면 기본 4로 표시(필요시 로직 교체)
            m.put("stars", star5FromReview(rv));

            String role = (rv.getMember() != null && rv.getMember().getRoleKind() != null)
                    ? rv.getMember().getRoleKind().name()
                    : "Member";
            m.put("role", role);

            m.put("summary", shorten(rv.getContent(), 120));
            m.put("cafeName", (rv.getCafe() != null) ? rv.getCafe().getName() : "카페");
            m.put("date", (rv.getCreatedAt() != null) ? rv.getCreatedAt().format(fmt) : "");

            return m;
        }).collect(Collectors.toList());
    }

    /** 홈/검색 공통 미션 섹션/로그인 컨텍스트 내려주기 */
    private void addMissionContext(Model model, Authentication auth) {
        boolean loggedIn = auth != null && auth.isAuthenticated();
        boolean isPro = false;

        if (loggedIn) {
            Optional<Member> opt = memberRepository.findByEmail(auth.getName());
            if (opt.isPresent()) {
                Member me = opt.get();
                // 리뷰/활동 이후 등급 변동 반영
                proGateService.refreshRoleKind(me.getId());
                isPro = (me.getRoleKind() != null) && "PRO".equalsIgnoreCase(String.valueOf(me.getRoleKind()));
            }
        }

        // CafeInfo.notice에 [미션] 포함된 공지만 미션 카드로 노출 (최대 6개)
        List<Map<String, Object>> missionsHome = new ArrayList<>();
        for (CafeInfo ci : cafeInfoRepository.findByNoticeMarker("[미션]")) {
            if (missionsHome.size() >= 6) break;

            Cafe cafe = ci.getCafe();
            String notice = Optional.ofNullable(ci.getNotice()).orElse("");
            boolean sponsored = notice.contains("[협찬]");
            LocalDate due = parseDue(notice);

            Map<String, Object> card = new HashMap<>();
            card.put("cafeId", cafe.getId());
            card.put("cafeName", cafe.getName());
            card.put("summary", shorten(notice, 120));
            card.put("notice", notice);
            card.put("due", due != null ? due.toString() : "-");
            card.put("sponsored", sponsored);

            missionsHome.add(card);
        }

        model.addAttribute("missionsHome", missionsHome);
        // 전역 advice가 isLoggedIn을 내려주지만, 과거 사용을 고려해 loggedIn도 잠시 유지
        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("isPro", isPro);
    }

    // ====== 유틸 ======

    private static String initialsOf(String nick) {
        if (nick == null || nick.isBlank()) return "·";
        return nick.substring(0, 1);
    }

    private static String star5(int rating) {
        int r = Math.max(0, Math.min(5, rating));
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) sb.append(i < r ? "★" : "☆");
        return sb.toString();
    }

    /** Review에 평점 필드가 없다면 4 고정(필요시 실제 필드로 교체) */
    private static String star5FromReview(Review rv) {
        // 예: rv.getRating() 이 있다면 사용: return star5(Optional.ofNullable(rv.getRating()).orElse(4));
        return star5(4);
    }

    private static LocalDate parseDue(String s) {
        if (s == null) return null;
        var m = Pattern.compile("DUE:(\\d{4}-\\d{2}-\\d{2})").matcher(s);
        return m.find() ? LocalDate.parse(m.group(1)) : null;
    }

    private static String shorten(String s, int n) {
        if (s == null) return "";
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() > n ? t.substring(0, n) + "…" : t;
    }
}
