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

    // ✅ 미션 섹션을 위해 추가 주입
    private final CafeInfoRepository cafeInfoRepository;
    private final MemberRepository memberRepository;
    private final ProGateService proGateService;

    @GetMapping({"/", "/main"})
    public String home(Model model, Authentication auth) {
        // ------ 기존: Top 8 카페 + 메인 사진 매핑 ------
        List<Cafe> cafes = cafeService.findApprovedTopByViews(8);
        Set<Long> topIds = cafes.stream().map(Cafe::getId).collect(Collectors.toSet());
        List<CafePhoto> mainPhotos = cafePhotoService.findForCafeIdsOrderByMainThenSort(topIds);

        Map<Long, String> photoByCafeId = new HashMap<>();
        for (CafePhoto p : mainPhotos) {
            Long cafeId = p.getCafe().getId();
            photoByCafeId.putIfAbsent(cafeId, p.getUrl()); // 첫 번째만 채택(메인 없으면 최선순)
        }

        final String PLACEHOLDER = "/images/placeholder-cafe.jpg";
        List<CafeCardForm> cafeCards = cafes.stream()
                .map(c -> {
                    String url = photoByCafeId.get(c.getId());
                    String safeUrl = (url == null || url.isBlank()) ? PLACEHOLDER : url;
                    return new CafeCardForm(
                            c.getId(), c.getName(), c.getAddress(),
                            c.getNumber(), c.getCode(), c.getViews(),
                            safeUrl
                    );
                })
                .collect(Collectors.toList());

        List<CafeTag> cafeTags = cafeTagService.findAll().stream()
                .filter(Objects::nonNull)
                .limit(24)
                .collect(Collectors.toList());

        List<Review> recentReviews = reviewService.findAll().stream()
                .sorted(Comparator.comparing(Review::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(6)
                .collect(Collectors.toList());

        model.addAttribute("cafeCards", cafeCards);
        model.addAttribute("cafeTags", cafeTags);
        model.addAttribute("recentReviews", recentReviews);

        // ------ ✅ 추가: 미션 섹션 + 로그인/Pro 컨텍스트 ------
        addMissionContext(model, auth);

        return "page/main";
    }

    @GetMapping("/search")
    public String search(@RequestParam(value = "q", required = false) String q,
                         @RequestParam(value = "tag", required = false) String tag,
                         @RequestParam(value = "category", required = false) String category,
                         Model model,
                         Authentication auth) { // ✅ auth 주입 받아 컨텍스트 일관화

        List<Cafe> results = cafeService.searchApproved(q);

        List<CafeTag> tags = cafeTagService.findAll().stream().limit(24).collect(Collectors.toList());
        List<Review> recent = reviewService.findAll().stream()
                .sorted(Comparator.comparing(Review::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(6)
                .collect(Collectors.toList());

        model.addAttribute("trendingCafes", results);
        model.addAttribute("cafeTags", tags);
        model.addAttribute("recentReviews", recent);
        model.addAttribute("query", q);
        model.addAttribute("selectedTag", tag);
        model.addAttribute("selectedCategory", category);

        // ------ ✅ 검색 페이지도 같은 메인 템플릿을 쓰므로 미션 컨텍스트 내려줌 ------
        addMissionContext(model, auth);

        return "page/main";
    }

    // ====== 내부 헬퍼 ======

    /** 홈/검색 공통으로 쓰는 미션 섹션 데이터 내려주기 */
    private void addMissionContext(Model model, Authentication auth) {
        boolean loggedIn = auth != null && auth.isAuthenticated();
        boolean isPro = false;

        if (loggedIn) {
            Optional<Member> opt = memberRepository.findByEmail(auth.getName());
            if (opt.isPresent()) {
                Member me = opt.get();
                // 리뷰 저장 후 등급 변동이 있을 수 있으니 최신화
                proGateService.refreshRoleKind(me.getId());
                isPro = "PRO".equalsIgnoreCase(me.getRoleKind());
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
            card.put("notice", notice); // 상세 모달에서 그대로 사용
            card.put("due", due != null ? due.toString() : "-");
            card.put("sponsored", sponsored);
            missionsHome.add(card);
        }

        model.addAttribute("missionsHome", missionsHome);
        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("isPro", isPro);
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